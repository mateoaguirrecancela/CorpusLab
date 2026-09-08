# Contribuir a CorpusLab

Gracias por el interés. CorpusLab nació como Trabajo de Fin de Grado y lo mantiene
una sola persona, así que el proceso es deliberadamente ligero: incidencias
concretas y _pull requests_ pequeñas y enfocadas.

## Antes de escribir código

- **Errores.** Cuenta qué esperabas, qué ocurrió y cómo reproducirlo. Añade la
  versión de Docker, Java o Node si es relevante.
- **Ideas y funcionalidades.** Describe primero el caso de uso y después la
  solución propuesta. Para cambios grandes —módulos nuevos, cambios de
  arquitectura— abre una incidencia para poder discutirlo: una _pull request_
  extensa sin acuerdo previo es difícil de aceptar.
- **Vulnerabilidades.** No las publiques en una incidencia; sigue el
  [procedimiento de seguridad](#seguridad).

## Seguridad

Si encuentras una vulnerabilidad, **no abras una incidencia pública**. Repórtala
de forma privada desde la pestaña _Security_ del repositorio, con _Report a
vulnerability_.

Los puntos recogidos en la [hoja de ruta](README.md#hoja-de-ruta) son alcance
asumido de la versión actual, no vulnerabilidades.

## Entorno

Los requisitos, la configuración y los comandos para arrancar el proyecto en
local están documentados en el [README](README.md#puesta-en-marcha).

## Flujo de trabajo

El repositorio sigue el modelo de ramificación
[Git Flow](https://nvie.com/posts/a-successful-git-branching-model/). Hay dos
ramas permanentes: `main`, que contiene las versiones publicadas, y `develop`,
la rama de integración sobre la que converge el trabajo en curso. Todas las
demás son efímeras: se crean para una tarea y se borran al fusionarlas.

Cada una se nombra `tipo/nombre-tarea` —por ejemplo,
`fix/kappa-fleiss-clases-vacias`—, donde el tipo declara su propósito:

| Tipo       | Para qué                                       | Sale de   | Vuelve a           |
| ---------- | ---------------------------------------------- | --------- | ------------------ |
| `feature`  | Funcionalidad nueva                            | `develop` | `develop`          |
| `fix`      | Corrección de un error                         | `develop` | `develop`          |
| `refactor` | Reestructuración sin cambiar el comportamiento | `develop` | `develop`          |
| `test`     | Añadir o mejorar pruebas                       | `develop` | `develop`          |
| `chore`    | Configuración, dependencias, mantenimiento     | `develop` | `develop`          |
| `release`  | Preparar y estabilizar una versión             | `develop` | `main` y `develop` |
| `hotfix`   | Corregir con urgencia un fallo ya publicado    | `main`    | `main` y `develop` |

Las dos últimas son de gestión de versiones y las lleva quien mantiene el
proyecto. **Una contribución externa parte siempre de `develop` y vuelve a
`develop`**, nunca a `main`.

1. Haz un _fork_ y crea tu rama a partir de `develop`, siguiendo la convención
   de nombres de la tabla.
2. Escribe el cambio respetando las
   [decisiones de arquitectura](#decisiones-de-arquitectura).
3. Añade o actualiza las pruebas de lo que toques.
4. Si cambias algún texto de la interfaz, actualiza la clave en los **tres**
   idiomas (`es`, `en`, `gl`). Los ficheros viven en los directorios `locales/`
   de cada módulo del frontend y en `src/shared/locales/`.
5. Comprueba en local antes de abrir la _pull request_:

   ```bash
   cd backend && ./mvnw test
   cd ../frontend && npm run lint && npm run test && npm run build
   ```

6. Abre la _pull request_ **contra `develop`**.

No hay integración continua, así que el paso 5 es la única red de seguridad que
existe antes de la revisión.

## Mensajes de _commit_

El formato de los mensajes de _commit_ es `[tipo] #incidencia Título:
descripción`, donde `tipo` puede ser `feature`, `fix`, `refactor`, `test` o
`chore`:

```
[fix] #42 Cálculo IAA: corregir Kappa de Fleiss con clases vacías
```

El número de incidencia es obligatorio salvo en cambios triviales que no tengan
una asociada; en ese caso omítelo, no inventes uno.

## Estilo

- **Backend.** Java 21. No hay formateador automático configurado,
  mantén el estilo del fichero y del módulo en los que trabajes.
- **Frontend.** TypeScript con Prettier y ESLint. Ejecuta `npm run format` antes
  de hacer _commit_; `npm run lint` comprueba las dos cosas.

## Pruebas

El backend usa JUnit 5, con Testcontainers para las pruebas de integración; el
frontend, Vitest con Testing Library.

Al añadir código nuevo, escribe la prueba al nivel más barato que cubra el
cambio: aísla la lógica siempre que puedas y reserva las pruebas que levantan
infraestructura para los flujos que de verdad la necesitan. Prueba el
comportamiento observable, no los detalles internos, y cubre también el caso de
error, no solo el camino feliz.

## Decisiones de arquitectura

Estas decisiones son deliberadas. Si crees que alguna debería cambiar, ábrelo
como incidencia para discutirlo antes de crear la _pull request_.

- **`project/iaa` usa arquitectura hexagonal** (`adapter`, `application`,
  `domain`, `port`), mientras que el resto de módulos del backend siguen el patrón
  plano: Controller → Service → ServiceImpl → Repository → DTO/Entity. La
  asimetría es intencionada: aísla el cálculo estadístico del CRUD y permite
  probarlo sin infraestructura. No se homogeneiza.
- **La autorización se aplica en la capa de servicio**, no con `@PreAuthorize`
  declarativo sobre los controladores.
- **Toda subida de ficheros pasa por `FileSecurityUtil`**, que valida el tipo
  real mediante Apache Tika y sanea los CSV frente a inyección de fórmulas. No
  añadas validaciones paralelas por tu cuenta.
- **Los cálculos de acuerdo entre anotadores no devuelven `NaN` ni lanzan
  excepciones** cuando los datos no dan para calcular: devuelven un estado
  explicativo (`INSUFFICIENT_ANNOTATORS`, `NO_SHARED_ITEMS`, `UNDEFINED`…) que
  la interfaz muestra al usuario. Mantén ese contrato.

## Idioma

El código, los _commits_ y la documentación están en castellano, que es el idioma
de trabajo del proyecto. Aun así, puedes abrir incidencias y _pull requests_ en
inglés si te resulta más cómodo: se responderá en el idioma en el que se abran.

## Licencia

Al enviar una _pull request_ aceptas que tu contribución se distribuya bajo la
[licencia Apache 2.0](LICENSE) del proyecto, conforme a su sección 5.
