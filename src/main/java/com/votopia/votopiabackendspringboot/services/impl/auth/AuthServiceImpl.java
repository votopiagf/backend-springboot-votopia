package com.votopia.votopiabackendspringboot.services.impl.auth;

import com.votopia.votopiabackendspringboot.dtos.auth.LoginRequestDto;
import com.votopia.votopiabackendspringboot.dtos.auth.LoginSummaryDto;
import com.votopia.votopiabackendspringboot.dtos.user.UserSummaryDto;
import com.votopia.votopiabackendspringboot.entities.Organization;
import com.votopia.votopiabackendspringboot.entities.User;
import com.votopia.votopiabackendspringboot.exceptions.UnauthorizedException;
import com.votopia.votopiabackendspringboot.repositories.OrganizationRepository;
import com.votopia.votopiabackendspringboot.repositories.UserRepository;
import com.votopia.votopiabackendspringboot.services.auth.AuthService;
import com.votopia.votopiabackendspringboot.services.auth.JwtService; // Importa l'interfaccia
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor // Sostituisce gli @Autowired sui campi, generando il costruttore (best practice)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService; // Usa l'interfaccia, non l'implementazione

    @Override
    @Transactional(readOnly = true)
    public LoginSummaryDto login(LoginRequestDto request) {
        log.info("Tentativo di login per l'email: {} con codice org: {}", request.getEmail(), request.getCodeOrg());

        // 1. Cerchiamo l'organizzazione tramite orgCode
        // Assicurati che il metodo nel repository si chiami findByCode
        Organization org = organizationRepository.findOrganizationByCode(request.getCodeOrg())
                .orElseThrow(() -> new UnauthorizedException("Codice Organizzazione non valido"));

        // 2. Cerchiamo l'utente che appartiene a QUELLA organizzazione con QUELLA email
        // Spring Data JPA preferisce findByEmailAndOrg
        User user = (User) userRepository.findUsersByEmailAndOrg(request.getEmail(), org)
                .orElseThrow(() -> new UnauthorizedException("Credenziali non valide per questa organizzazione"));

        // 3. Verifica della password (hash matching)
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Password errata per l'utente: {}", request.getEmail());
            throw new UnauthorizedException("Password errata");
        }

        // 4. Se tutto è ok, generiamo il Token
        String token = jwtService.generateToken(user);

        log.info("Login effettuato con successo per l'utente: {}", user.getId());

        // 5. Restituiamo il DTO
        return new LoginSummaryDto(token, new UserSummaryDto(user));
    }
}