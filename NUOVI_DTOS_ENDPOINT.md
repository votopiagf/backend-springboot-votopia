# Nuovi DTOs e Endpoint per Dropdown di Liste e Ruoli

## DTOs Creati

### 1. `ListOptionDto`
**Path:** `src/main/java/com/votopia/votopiabackendspringboot/dtos/list/ListOptionDto.java`

Utilizzato per i dropdown di liste durante la creazione di un utente.
- **Campi:** `id`, `name`, `school`
- **Uso:** Mostra solo le informazioni minime necessarie (ID, nome lista, scuola/organizzazione)

### 2. `RoleOptionDto`
**Path:** `src/main/java/com/votopia/votopiabackendspringboot/dtos/role/RoleOptionDto.java`

Utilizzato per i dropdown/checkbox di ruoli durante la creazione di un utente.
- **Campi:** `id`, `name`, `color`, `listName`
- **Uso:** Mostra informazioni essenziali del ruolo (ID, nome, colore, nome della lista se applicabile)

### 3. `UserCreationInitDto` ⭐ **NUOVO**
**Path:** `src/main/java/com/votopia/votopiabackendspringboot/dtos/user/UserCreationInitDto.java`

DTO che contiene **TUTTI i dati necessari per inizializzare la schermata di creazione utente in un'unica richiesta**.
- **Campi:**
  - `availableLists` - Set di `ListOptionDto`
  - `availableRoles` - Set di `RoleOptionDto` (ruoli a livello organizzazione)
  - `availableRolesByList` - Set di `RoleOptionDto` (ruoli per lista, se richiesti)
- **Uso:** Restituito da un unico endpoint per evitare multiple richieste HTTP al caricamento della pagina

## Metodi di Servizio

### ListService

#### `getAssignableListsForUserCreation(Long authUserId)`
Restituisce le liste che l'utente autenticato può assegnare durante la creazione di un utente, rispettando i suoi permessi specifici.

**Logica:**
- Se ha `create_user_for_organization` → vede tutte le liste dell'organizzazione
- Se ha solo `create_user_for_list` → vede solo le liste su cui ha il permesso `create_user_for_list`

**Return:** `Set<ListOptionDto>`

### RoleService

#### `getAssignableRolesForUserCreation(Long authUserId, @Nullable Long targetListId)`
Restituisce i ruoli che l'utente autenticato può assegnare durante la creazione di un utente.

**Logica:**
- **Se `targetListId` è NULL (creazione a livello Org):**
  - Richiede il permesso `create_user_for_organization`
  - Ritorna solo ruoli globali (ORG)
  - Filtra per livello < massimo livello dell'utente

- **Se `targetListId` è fornito (creazione in una lista specifica):**
  - Richiede `create_user_for_organization` oppure `create_user_for_list` sulla lista
  - Ritorna ruoli della lista specifica
  - Se ha permesso org-wide, aggiunge anche ruoli globali
  - Filtra per livello < massimo livello nel contesto

**Return:** `Set<RoleOptionDto>`

## Endpoint API

### GET `/api/users/init-creation/` ⭐ **ENDPOINT PRINCIPALE**
Ottiene **TUTTI i dati necessari per inizializzare la schermata di creazione utente** in un'unica richiesta.

**Request:**
```
GET /api/users/init-creation/
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "data": {
    "availableLists": [
      {
        "id": 1,
        "name": "Lista A",
        "school": "Liceo Scientifico Milano"
      },
      {
        "id": 2,
        "name": "Lista B",
        "school": "Liceo Classico Roma"
      }
    ],
    "availableRoles": [
      {
        "id": 1,
        "name": "Admin",
        "color": "#FF0000",
        "listName": null
      },
      {
        "id": 2,
        "name": "Editor",
        "color": "#00FF00",
        "listName": null
      }
    ],
    "availableRolesByList": []
  },
  "message": "Dati di inizializzazione ottenuti con successo",
  "timestamp": 1704614400000
}
```

---

### GET `/api/users/options/lists`
Ottiene le liste assegnabili per la creazione di un utente.

**Request:**
```
GET /api/users/options/lists
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "data": [
    {
      "id": 1,
      "name": "Lista A",
      "school": "Liceo Scientifico"
    },
    {
      "id": 2,
      "name": "Lista B",
      "school": "Liceo Classico"
    }
  ],
  "message": "Liste disponibili ottenute con successo",
  "timestamp": 1704614400000
}
```

---

### GET `/api/users/options/roles?target_list_id=<id>`
Ottiene i ruoli assegnabili per la creazione di un utente.

**Request Parameters:**
- `target_list_id` (optional): ID della lista target. Se omesso, restituisce ruoli a livello organizzazione.

**Request:**
```
GET /api/users/options/roles
Authorization: Bearer <token>

# O con parametro:
GET /api/users/options/roles?target_list_id=1
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "data": [
    {
      "id": 1,
      "name": "Admin",
      "color": "#FF0000",
      "listName": null
    },
    {
      "id": 2,
      "name": "Editor",
      "color": "#00FF00",
      "listName": "Lista A"
    }
  ],
  "message": "Ruoli disponibili ottenuti con successo",
  "timestamp": 1704614400000
}
```

---

## Utilizzo nel Frontend

### ✅ METODO CONSIGLIATO: Caricamento iniziale unico

Carica **TUTTI i dati in una sola richiesta** al caricamento della pagina:

```typescript
// Al caricamento della pagina di creazione utente
const response = await fetch('/api/users/init-creation/', {
  headers: { 'Authorization': 'Bearer ' + token }
});
const { data: initData } = await response.json();

// initData contiene:
// - initData.availableLists: liste disponibili
// - initData.availableRoles: ruoli organizzazione
// - initData.availableRolesByList: (vuoto di base, si può popolare dinamicamente)

// Popolare i dropdown
const listSelect = document.getElementById('listSelect');
initData.availableLists.forEach(list => {
  const option = document.createElement('option');
  option.value = list.id;
  option.text = `${list.name} (${list.school})`;
  listSelect.appendChild(option);
});

// Popolare i ruoli
const roleSelect = document.getElementById('roleSelect');
initData.availableRoles.forEach(role => {
  const option = document.createElement('option');
  option.value = role.id;
  option.text = role.name;
  option.style.backgroundColor = role.color;
  roleSelect.appendChild(option);
});
```

---

### 🔄 METODO ALTERNATIVO: Caricamenti separati (se necessario)

Se per qualche motivo preferisci caricamenti separati:

```typescript
// Fetch delle liste disponibili
const response = await fetch('/api/users/options/lists', {
  headers: { 'Authorization': 'Bearer ' + token }
});
const { data: lists } = await response.json();

// lists ora contiene: [{ id, name, school }, ...]
// Usare per popolare un <select> o <multi-select>
```

```typescript
// Fetch dei ruoli disponibili
const response = await fetch('/api/users/options/roles', {
  headers: { 'Authorization': 'Bearer ' + token }
});
const { data: roles } = await response.json();

// roles ora contiene: [{ id, name, color, listName }, ...]
// Usare per popolare un <multi-select> con checkbox
```

```typescript
// Popolare ruoli per una lista specifica (cambio dinamico)
const listId = 1;
const response = await fetch(`/api/users/options/roles?target_list_id=${listId}`, {
  headers: { 'Authorization': 'Bearer ' + token }
});
const { data: rolesForList } = await response.json();

// rolesForList contiene i ruoli assegnabili per la lista specifica
```

---

## Flusso di Creazione Utente Consigliato

1. **Carica le liste disponibili** → GET `/api/users/options/lists`
2. **Mostra dropdown liste** → Utente seleziona una o più liste
3. **Al cambiare lista selezionata:**
   - Carica i ruoli per quella lista → GET `/api/users/options/roles?target_list_id=<selectedListId>`
   - Aggiorna il dropdown dei ruoli
4. **Utente seleziona ruoli** → Multi-select con checkbox
5. **Invia creazione utente** → POST `/api/users/register/` con:
   ```json
   {
     "name": "...",
     "surname": "...",
     "email": "...",
     "password": "...",
     "listsId": [1, 2],
     "rolesId": [5, 6]
   }
   ```

---

## Note di Sicurezza

- Entrambi gli endpoint verificano i **permessi dell'utente autenticato**
- Le liste e i ruoli sono filtrati in base ai permessi specifici
- I ruoli sono ulteriormente filtrati per livello gerarchico (non puoi assegnare ruoli di livello >= al tuo)
- Multi-tenancy è garantita: nessuno può vedere/assegnare liste o ruoli di altre organizzazioni

