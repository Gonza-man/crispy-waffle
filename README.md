# Crispy Waffle - Mueblería CRUD

## 🎯 Patrones de Diseño Implementados

1. **State Pattern:** Orden con estados COTIZACION → VENTA → CANCELADA
2. **Decorator Pattern:** Variantes que decoran muebles base
3. **Strategy Pattern:** Cálculo de precios (FIJO vs PORCENTAJE)
4. **Snapshot Pattern:** Congelación de precios al confirmar venta

## 🚀 Quick Start

```bash
# Iniciar todos los servicios
docker-compose up --build

# Acceder a:
# - Frontend: http://localhost:5000
# - API: http://localhost:8080
# - phpMyAdmin: http://localhost:8081
```

## 🏗️ Arquitectura

### Backend (Spring Boot + Java 21)

- REST API con Spring Boot 3.5.7
- JPA/Hibernate + MariaDB
- Lombok para reducir boilerplate
- Patrones implementados en capa de servicio

### Frontend (Flask + HTMX)

- Flask 3.1.2 con templates Jinja2
- HTMX para interactividad sin JavaScript
- UV para gestión de dependencias Python
- CSS puro sin frameworks

## 📂 Estructura

```
crispy-waffle/
├── api/                    # Backend Spring Boot
│   ├── src/main/java/
│   │   └── cl/ubiobio/muebleria/
│   │       ├── controllers/
│   │       ├── services/
│   │       ├── repositories/
│   │       ├── models/
│   │       ├── dto/
│   │       ├── strategy/
│   │       └── enums/
│   └── pom.xml
├── frontend/               # Frontend Flask + HTMX
│   ├── app.py
│   ├── templates/
│   └── pyproject.toml
├── docker-compose.yaml
└── README.md
```

## 🧪 Tests

### Ejecutar Tests

```bash
cd api
mvn test

# O con Docker
docker run --rm -v $(pwd)/api:/app -w /app \
  maven:3.9.11-eclipse-temurin-21 mvn test
```

### Resultados

- **47 tests** ✓ 100% passed
- **4 patrones** totalmente testeados
- **Unit tests** + Integration tests
- **Coverage:** Strategy (100%), Services (90%+)

## 📊 Base de Datos

- **Engine:** MariaDB
- **Gestión:** Hibernate DDL auto-update
- **Acceso:** phpMyAdmin en puerto 8081

