# API Microcréditos

*[English version](README.en.md)*

Core de una API de microcréditos: registro y autenticación de usuarios, solicitud
y desembolso de créditos, generación de la tabla de amortización, y recaudo con
imputación de pagos en cascada.

Proyecto personal, en desarrollo.

## Stack

Java 21 · Spring Boot 4 · Spring Modulith · PostgreSQL · Flyway · Testcontainers · Docker

## Módulos

| Módulo | Responsabilidad |
|---|---|
| `iam` | Usuarios, roles, autenticación JWT y rotación de refresh tokens |
| `operational` | Productos de crédito, originación, cronograma de pagos y recaudo |
| `common` | Entidad base, manejo de errores y el principal compartido |

Los límites entre módulos los verifica Spring Modulith en un test: `operational`
solo alcanza `iam` a través de una interfaz publicada, nunca por sus repositorios.

## Flujo del crédito

Antes de todo, un admin registra la tasa de usura que certifica la
Superfinanciera cada mes (`POST /admin/usury-rate`) y crea los productos
(`POST /admin/product`). Un producto se rechaza si su interés corriente o su
mora superan la usura vigente de su modalidad.

1. Un admin verifica la identidad del usuario
   (`PATCH /admin/users/{username}/identity-verification`).
2. El usuario consulta el catálogo de productos.
3. Solicita uno. El sistema lo rechaza si no tiene la identidad verificada, si
   está en mora, si su score no alcanza, o si ya tiene otra solicitud en curso.
4. Si pasa, el crédito queda pre-aprobado. Puede aceptarlo o cancelarlo.
5. Al aceptarlo se genera la tabla de amortización completa.
6. Registra un pago contra el crédito, no contra una cuota concreta.
7. Un admin confirma el pago (`POST /admin/payment/validate`) y se imputa en
   cascada: primero mora, luego intereses, luego capital, y lo que sobre pasa a
   las cuotas siguientes.

Un job diario, en este orden, mueve las cuotas a vigente o vencida, causa
intereses de mora y actualiza el estado del crédito.

## Decisiones de diseño

- **Los refresh tokens se guardan hasheados** (SHA-256). Un volcado de la base de
  datos no permite suplantar a nadie.
- **La rotación se serializa con un lock pesimista**, con una ventana de gracia
  para que dos peticiones simultáneas legítimas no se confundan con un ataque.
  Reutilizar un token fuera de esa ventana revoca todos los del usuario.
- **La mora se causa de forma incremental y nunca supera la usura.** Cada día
  nuevo suma `capital vencido pendiente × tasa diaria`, donde la tasa diaria es
  `(1 + EA)^(1/365) − 1` sobre `min(mora del producto, usura vigente ese día)`.
  Si la mora cruza un cambio de mes, cada tramo usa la tasa de su mes. Lo ya
  causado nunca se recalcula, así que un abono a capital reduce la base hacia
  adelante, no hacia atrás. Correr el job dos veces el mismo día no suma nada, y
  antes de imputar un pago la mora se pone al día. Las fechas se toman del
  `Clock` en hora de Colombia (`America/Bogota`).
- **La usura depende de la modalidad.** Cada producto tiene una modalidad de
  crédito (consumo y ordinario, bajo monto, productivo, popular productivo…)
  porque la Superfinanciera certifica un tope distinto para cada una. Una tasa
  rige desde el primer día de su mes hasta que se registra la siguiente.
- **Amortización alemana** (capital fijo, cuota decreciente). La última cuota
  absorbe el residuo del redondeo para que el capital cuadre exacto.
- **Un pago no pertenece a una cuota.** Se reparte entre conceptos y cuotas, y
  cada tramo queda registrado como una imputación con su concepto.
- **Un pago solo sale de `PENDING` una vez.** `PENDING → APPROVED` o
  `PENDING → DECLINED`; los dos son finales. Repetir la misma validación devuelve
  el resultado original, y pedir la contraria responde 409.
- **La validación bloquea el pago y luego su crédito** (`SELECT ... FOR UPDATE`),
  siempre en ese orden. Dos confirmaciones simultáneas del mismo pago no lo
  imputan dos veces, y dos pagos del mismo crédito no se pisan las cuotas.
- Los módulos se referencian entre sí por un UUID público, nunca por la llave
  primaria interna.
- Los errores se devuelven como `ProblemDetail` (RFC 7807) con un código estable.

## Cómo levantarlo

```bash
cp .env.example .env     # credenciales de la base de datos y JWT_KEY (Base64, 256 bits)
docker compose up -d --build
```

Levanta PostgreSQL y la aplicación. La API queda en `http://localhost:8080` y la
documentación en `/swagger-ui.html`.

Si `.env` define `ADMIN_USERNAME`, `ADMIN_EMAIL` y `ADMIN_PASSWORD`, al arrancar
se crea esa cuenta con rol `ADMIN` (si no existe ya). Cambia la contraseña de
ejemplo antes de desplegar.

Para correr la aplicación desde el IDE, levanta solo la base de datos con
`docker compose up -d db` y apunta el datasource a `localhost:5432`.

## Tests

```bash
./mvnw verify            # requiere Docker
```

Más de 130 tests, en tres niveles:

- **Unitarios** — firma y parseo de JWT, hasheo de tokens y cada rama de la
  lógica de rotación, incluido el borde exacto del periodo de gracia, y cada
  transición de estado de un pago. En `operational`, la causación de la mora
  (el escenario del abono parcial, idempotencia, días recuperados y cambio de
  mes), la conversión E.A. a diaria con el tope de usura, la imputación en
  cascada, la tabla de amortización y las reglas de productos y tasas de usura.
- **Integración** (Testcontainers) — las queries contra Postgres real, que la
  revocación sobreviva a la excepción, y dos rotaciones concurrentes del mismo
  token resolviendo en exactamente un token nuevo. En `operational`, dos
  validaciones concurrentes del mismo pago imputándolo una sola vez, y el saldo
  del crédito cuadrando con el capital de sus cuotas. El job diario sobre una
  base real: mora con la usura de la migración, sin recalcularse después de un
  abono y sin duplicarse si corre dos veces el mismo día.
- **Slice web** — el contrato HTTP de `/auth` y la cadena de seguridad: sin
  token, token expirado, token falsificado y acceso por rol, incluidos los
  endpoints de admin.

## Limitaciones conocidas

- Sin rate limiting en el login.
- Sin pasarela de pagos real. `/admin/payment/validate` simula la
  confirmación del proveedor.
- Sin abono a capital con re-amortización: pagar de más cubre cuotas futuras.
- El excedente se registra como imputación pero no es todavía un saldo a favor
  utilizable.
- Las tasas de usura se registran a mano: si falta la de un mes nuevo se sigue
  aplicando la anterior, y corregir una tasa ya usada no recalcula la mora
  causada con ella. La migración trae las de 2026 para consumo y ordinario, y
  septiembre y octubre para las demás modalidades.

## Historial

- [#1 — Módulo IAM](https://github.com/carlosescobar30/api-microcreditos/pull/1)
- [#2 — Módulo Operational](https://github.com/carlosescobar30/api-microcreditos/pull/2)
