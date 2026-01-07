# 🚀 Votopia Backend - Spring Boot

Un'architettura **enterprise-grade** con pattern **Adapter** e **Use Cases** per la gestione di utenti, liste, ruoli e permessi in un sistema di voting.

---

## 📋 Architettura

```
src/main/java/com/votopia/votopiabackendspringboot/
├── adapters/                 # Pattern Adapter - Conversione dati
│   └── dtos/
│       ├── ListDtoAdapter.java
│       ├── RoleDtoAdapter.java
│       └── UsersScreenDtoAdapter.java
├── usecases/                 # Logica di business (Application Services)
│   ├── GetAvailableListsUseCase.java
│   ├── GetAvailableRolesUseCase.java
│   ├── GetUserCreationInitUseCase.java
│   └── GetUsersScreenInitUseCase.java
├── controllers/              # HTTP Layer - Endpoints REST
├── services/                 # Orchestrazione e logica complessa
├── repositories/             # Data Access Layer
├── entities/                 # Domain Objects
├── dtos/                     # Data Transfer Objects
├── config/                   # Configurazione (Security, JWT, ecc)
└── exceptions/               # Custom Exceptions
```

---

## 🏗️ Pattern Utilizzati

### 1. **Adapter Pattern**
Responsabili della conversione di dati dal dominio (entities) alla presentazione (DTOs).
```java
// ListDtoAdapter converte List entity → ListOptionDto
ListOptionDto dto = listDtoAdapter.toOptionDto(list);
```

### 2. **Use Case / Application Service**
Encapsulano la logica di business. Ogni use case ha una singola responsabilità.
```java
// GetUsersScreenInitUseCase ritorna tutti i dati per la schermata Users
UsersScreenInitDto data = getUsersScreenInitUseCase.execute(userId);
```

### 3. **Layered Architecture**
Separazione chiara tra HTTP → Business Logic → Data Access.

---

## 📚 Endpoint Principali

### 1. Schermata Users - Dati Completi
```
GET /api/users/init-screen/
Authorization: Bearer {token}
```
**Restituisce:**
- Liste disponibili (filtrate per permessi)
- Ruoli organizzazione
- Ruoli di lista (con info sulla lista di appartenenza)
- Statistiche (totale utenti, ruoli, liste)
- Scope di filtro (cosa può filtrare l'utente)

### 2. Form Creazione Utente
```
GET /api/users/init-creation/
Authorization: Bearer {token}
```
**Restituisce:**
- Liste disponibili
- Ruoli disponibili
- Ruoli per lista (se richiesti)

### 3. Opzioni Liste
```
GET /api/users/options/lists
Authorization: Bearer {token}
```

### 4. Opzioni Ruoli
```
GET /api/users/options/roles?target_list_id={listId}
Authorization: Bearer {token}
```

---

## 🔒 Sicurezza e Permessi

### Due Scenari di Accesso

**Scenario 1: Amministratore Organizzazione**
- Permesso: `view_all_user_organization`
- Vede: **TUTTE** le liste, **TUTTI** i ruoli, **TUTTI** gli utenti dell'org
- Può filtrare: Per qualsiasi lista

**Scenario 2: Moderatore Lista**
- Permesso: `view_all_user_list`
- Vede: **SOLO** la sua lista, **SOLO** i ruoli della sua lista
- Può filtrare: **SOLO** la sua lista
- UI informata della restrizione

---

## 🛠️ Setup e Avvio

### Prerequisiti
- Java 21+
- Maven 3.8+
- MySQL 8.0+

### Build
```bash
./mvnw clean package
```

### Run
```bash
./mvnw spring-boot:run
```

### Test Endpoint
```bash
curl -X GET http://localhost:8080/api/users/init-screen/ \
  -H "Authorization: Bearer {your-jwt-token}"
```

---

## 📊 Database

### Tabella Critica: `user_lists`
Relazione Many-to-Many tra User e List.

Se la tabella non esiste, crea manualmente:
```sql
CREATE TABLE IF NOT EXISTS user_lists (
    user_id BIGINT NOT NULL,
    list_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, list_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (list_id) REFERENCES lists(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 🎯 Flusso di Dati Tipico

```
Client (Frontend)
    ↓
GET /api/users/init-screen/
    ↓
UserController.initializeUsersScreen()
    ↓
GetUsersScreenInitUseCase.execute()
    ├─ Verifica permessi (PermissionService)
    ├─ Recupera dati (Repositories)
    └─ Delega conversione (UsersScreenDtoAdapter)
    ↓
Response (SuccessResponse<UsersScreenInitDto>)
    ↓
Client renderizza UI
```

---

## 📦 Dipendenze Principali

- **Spring Boot 3.x** - Framework web
- **Spring Data JPA** - ORM
- **MySQL Connector** - Database
- **JWT** - Autenticazione
- **Lombok** - Code generation
- **Swagger/OpenAPI** - Documentazione

---

## 📝 Configurazione

File: `src/main/resources/application.properties`

**Variabili d'ambiente richieste:**
- `DB_HOST` - Host MySQL
- `DB_PORT` - Porta MySQL
- `DB_NAME` - Nome database
- `DB_USER` - Username MySQL
- `DB_PASSWORD` - Password MySQL
- `SECRET_KEY` - Chiave JWT (min 32 caratteri)

---

## ✨ Status

- ✅ Architettura enterprise implementata
- ✅ Pattern Adapter integrato
- ✅ Use Cases creati e funzionanti
- ✅ Controller refactorizzato
- ✅ Endpoint testabili
- ⏳ Frontend: Integrazione richiesta

---

**Versione:** 1.0.0
**Data:** Gennaio 2026
**Author:** Votopia Team

