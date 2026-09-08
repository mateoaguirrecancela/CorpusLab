# CorpusLab

[![Versión](https://img.shields.io/badge/versión-1.0.0-green.svg)](CITATION.cff)
[![Licencia: Apache 2.0](https://img.shields.io/badge/licencia-Apache%202.0-blue.svg)](LICENSE)

Plataforma web colaborativa para la gestión de proyectos de anotación de datos en
investigación. Reúne en un único flujo controlado lo que habitualmente se resuelve
con hojas de cálculo y herramientas genéricas: configurar el proyecto, cargar el
conjunto de datos, redactar la guía de anotación, asignar participantes, anotar,
medir el acuerdo entre anotadores y exportar los resultados.

Desarrollada como Trabajo de Fin de Grado del Grao en Enxeñaría Informática de la
[Universidade da Coruña](https://www.udc.es).

## Características

- **Proyectos guiados por pasos**, con la configuración validada antes de que
  empiece la anotación.
- **Conjuntos de datos** en CSV, JSON, TXT, PDF e imágenes, con validación del
  tipo real de fichero y saneado de CSV frente a inyección de fórmulas.
- **Cuatro tipos de anotación**: clasificación simple, clasificación
  multietiqueta, detección de entidades sobre el texto y generación de texto
  libre.
- **Guías de anotación** accesibles para el anotador durante la propia tarea.
- **Acuerdo entre anotadores**: Kappa de Cohen, Kappa de Fleiss, Alfa de
  Krippendorff, xRR y F1 de solapamiento de _spans_. Cuando una métrica no se
  puede calcular, el sistema explica por qué en lugar de devolver un número sin
  sentido.
- **Grupos de investigación** con invitaciones por correo, asignación de
  participantes y seguimiento del progreso de cada anotador.
- **Exportación** de las anotaciones para su análisis posterior.
- **Autenticación** con credenciales propias o mediante Google y GitHub.
- **Interfaz en tres idiomas**: castellano, inglés y gallego.
- **Observabilidad** con Prometheus y Grafana incluidos en el despliegue.

## Tecnologías

| Capa            | Tecnologías                                                           |
| --------------- | --------------------------------------------------------------------- |
| Backend         | Java 21, Spring Boot 4, Spring Security, Spring Data JPA, Apache Tika |
| Frontend        | React 19, TypeScript, Vite, TailwindCSS 4, TanStack Query             |
| Datos           | PostgreSQL 16, Redis 7                                                |
| Infraestructura | Docker Compose, Caddy 2, Prometheus, Grafana                          |

## Puesta en marcha

Para levantar CorpusLab entero, en un servidor o en tu propia máquina, consulta
la guía completa en **[DEPLOY.md](DEPLOY.md)**.

A continuación se describe la puesta en marcha para **trabajar sobre el código en
local**.

### Requisitos

Java 21, Node 24, Docker y Docker Compose v2.

### Configuración

```bash
git clone https://github.com/mateoaguirrecancela/CorpusLab.git
cd CorpusLab
cp .env.example .env
```

> [!IMPORTANT]
> El fichero `.env` de la raíz es obligatorio también en local: el backend lo
> importa al arrancar y falla si le falta cualquier variable. Los valores de
> ejemplo sirven para desarrollo; el significado de cada uno está documentado en
> [DEPLOY.md](DEPLOY.md#3-configuración-del-entorno).
>
> La excepción es el login con Google y GitHub, que necesita credenciales reales
> y el dominio registrado en cada proveedor: se prueba levantando CorpusLab
> entero, como se explica en [DEPLOY.md](DEPLOY.md), y no contra el servidor de
> desarrollo.

### Backend

Levanta primero PostgreSQL y Redis:

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d postgres redis
```

Y después el backend:

```bash
cd backend
./mvnw spring-boot:run
```

Queda escuchando en `http://localhost:8080`.

> [!NOTE]
> Hacen falta los dos ficheros porque `docker-compose.yml` está pensado para
> producción y mantiene PostgreSQL y Redis dentro de la red interna de Docker,
> sin publicar sus puertos. `docker-compose.dev.yml` añade justo eso, para que
> el backend pueda alcanzarlos desde tu máquina. No lo uses en un servidor.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Queda escuchando en `http://localhost:5173`.

### Pruebas

**Backend**, con JUnit 5 y Testcontainers:

```bash
cd backend
./mvnw test
```

Las pruebas de integración levantan un PostgreSQL real con Testcontainers, de
modo que **Docker tiene que estar en marcha**. La cobertura se genera
automáticamente con JaCoCo en cada ejecución, el informe se guarda en
`backend/target/site/jacoco/index.html`.

**Frontend**, con Vitest y Testing Library:

```bash
cd frontend
npm run test
```

Para generar la cobertura hay que especificarlo al ejecutar los tests. El
informe se guarda en `frontend/coverage/index.html`:

```bash
npm run test:coverage
```

## Estructura

```
backend/                 API REST en Spring Boot
frontend/                Interfaz en React + TypeScript
observability/           Configuración de Prometheus y paneles de Grafana
docker-compose.yml       Despliegue de producción
docker-compose.dev.yml   Complemento para desarrollo local
```

Backend y frontend comparten el mismo particionado en módulos funcionales
—`auth`, `researchgroup`, `dashboard`, `notification` y `project`—, de manera que
una funcionalidad se toca en el mismo sitio a los dos lados. `project` es el
núcleo, con submódulos para la configuración, el conjunto de datos, la guía, los
participantes, la anotación, el progreso, el cálculo de acuerdo, las métricas y
la exportación.

Antes de tocar el código conviene leer las
[decisiones de arquitectura](CONTRIBUTING.md#decisiones-de-arquitectura) que el
proyecto mantiene a propósito.

## Contribuir

Las incidencias y las _pull requests_ son bienvenidas.
[CONTRIBUTING.md](CONTRIBUTING.md) recoge el flujo de trabajo, la convención de
_commits_, el estilo de código y las decisiones de arquitectura que conviene
respetar.

## Hoja de ruta

CorpusLab cubre hoy el ciclo completo de anotación. Estas son las líneas por las
que va a crecer, y donde una contribución tendría más impacto:

- **Migraciones de esquema versionadas**, con Flyway o Liquibase, para dar
  control de versiones a la base de datos en despliegues con datos reales.
- **Sesiones más largas**: renovación del token y guardado local de la anotación
  en curso, pensando en campañas de varias horas.
- **Anotación combinada** sobre un mismo elemento, para aplicar varios tipos de
  anotación a la vez sobre el mismo texto, en lugar de uno solo por proyecto.
- **Evaluación de modelos frente al criterio humano**, aprovechando xRR para
  medir el acuerdo entre las anotaciones de un modelo de lenguaje y las de los
  anotadores humanos.

## Licencia

Distribuido bajo la licencia [Apache 2.0](LICENSE). Copyright 2026 Mateo Aguirre
Cancela.

CorpusLab es software académico y se distribuye sin garantías de ningún tipo,
según establece la licencia. Quien lo despliegue es responsable de su
configuración: secretos propios, HTTPS, copias de seguridad y actualización de
las imágenes.

## Cómo citar

Si utilizas CorpusLab en un trabajo académico, cítalo mediante
[CITATION.cff](CITATION.cff). GitHub genera el formato APA o BibTeX desde el
botón «Cite this repository» de la página del repositorio.
