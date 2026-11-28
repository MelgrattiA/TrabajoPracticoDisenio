# TrabajoPracticoDiseño
Trabajo Practico de Diseño de Sistemas entrega 7 y 8
# Pasos para ejecutar el programa:
- Puede ser necesario usar más de una terminal
## Levantar la DB (PostgresSQL)
### En Linux:
1. Iniciar PostgresSQL `sudo systemctl start postgresql`
   - Opcional verificar que inicio `pg_isready -h localhost -p 5432`
2. Si es la primera vez se debe ejecutar:
   - creación del usuario `sudo -u postgres psql -c "CREATE USER admin WITH SUPERUSER PASSWORD 'admin';"`
   - creación de la base `sudo -u postgres psql -c "CREATE DATABASE gestion_evaluaciones OWNER admin;"`
### En Windows(PowerShell como administrador):
1. Iniciar PostgresSQL `net start postgresql-x64-XX` (XX remplazar por número de versión instalada)
2. Si es la primera vez se debe ejecutar:
   - creación del usuario `psql -U postgres -c "CREATE USER admin WITH SUPERUSER PASSWORD 'admin';"`
   - creación de la base `psql -U postgres -c "CREATE DATABASE gestion_evaluaciones OWNER admin;"`
## Descargar y levantar Backend 
1. Clonar el repositorio `git clone https://github.com/MelgrattiA/TrabajoPracticoDisenio.git`
2. Moverse a la carpeta del programa `cd TrabajoPracticoDisenio`
      ### En Linux:
      - Ejecutar el backend con `DB_USERNAME=admin DB_PASSWORD=admin ./mvnw spring-boot:run`
        - Opcional si es necesario dar permisos `chmod +x mvnw`
      ### En Windows(PowerShell):
      - Ejecutar el backend con `$env:DB_USERNAME="admin"; $env:DB_PASSWORD="admin"; .\mvnw.cmd spring-boot:run`
## Cargar DB (única vez)
### En Linux:
- Ejecutamos `PGPASSWORD=admin psql -U admin -h localhost -d gestion_evaluaciones -f inicializar_db_postgres.txt`
### En Windows(PowerShell):
- Ejecutamos `psql -U admin -h localhost -d gestion_evaluaciones -f inicializar_db_postgres.txt`
## Levantar Frontend
1. Moverse a la carpeta del front `cd frontend/my-app/`
2. Instalamos npm (por única vez) `npm install`
3. Ejecutamos `npm run dev`
# Direcciones de la aplicación
- Evaluaciones: http://localhost:3000/api/evaluar
- Preguntas: http://localhost:3000/api/preguntas
- Orden Merito: http://localhost:3000/api/orden-merito
- Completar Cuestionario: http://localhost:3000/api/cuestionario/instrucciones?clave=
