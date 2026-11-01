const ref = (name) => ({ $ref: `#/components/schemas/${name}` });
const list = (name) => ({ type: 'array', items: ref(name) });

const response = (description, schema) => ({
  description,
  content: { 'application/json': { schema } },
});

const errorResponse = (description) =>
  response(description, ref('Error'));

const requestBody = (schema) => ({
  required: true,
  content: { 'application/json': { schema: ref(schema) } },
});

const idParam = {
  name: 'id',
  in: 'path',
  required: true,
  description: 'Identificador del recurso (entero positivo).',
  schema: { type: 'integer', format: 'int64', minimum: 1 },
};

const activosParam = {
  name: 'soloActivos',
  in: 'query',
  required: false,
  description: 'true devuelve solo registros activos. Omitido o false devuelve todos.',
  schema: { type: 'boolean', default: false },
};

function operation(tag, summary, status, schema, options = {}) {
  const result = {
    tags: [tag],
    summary,
    responses: {
      [status]: response(options.success || 'Operación exitosa', schema),
    },
  };

  if (options.description) result.description = options.description;
  if (options.params) result.parameters = options.params;
  if (options.body) result.requestBody = requestBody(options.body);

  if (tag !== 'Autenticacion') {
    result.security = [{ bearerAuth: [] }];
    result.responses[401] = errorResponse('Sesion ausente, invalida o vencida.');
    result.responses[403] = errorResponse('El rol autenticado no tiene permisos para esta operacion.');
  }

  for (const [code, description] of Object.entries(options.errors || {})) {
    result.responses[code] = errorResponse(description);
  }

  return result;
}

const vehicleFields = {
  marca: { type: 'string', example: 'Toyota' },
  modelo: { type: 'string', example: 'Corolla' },
  anio: { type: 'integer', example: 2022 },
  color: { type: 'string', example: 'Blanco' },
  tipo: {
    type: 'string',
    enum: ['SEDAN', 'SUV', 'PICKUP', 'COUPE', 'HATCHBACK'],
    example: 'SEDAN',
  },
  precio_diario: {
    type: 'number', format: 'double', example: 45000,
  },
};

const customerFields = {
  documento: { type: 'string', example: '30111222' },
  nombre: { type: 'string', example: 'Ana' },
  apellido: { type: 'string', example: 'García' },
  email: {
    type: 'string', format: 'email', example: 'ana@example.com',
  },
  telefono: { type: 'string', example: '3815551234' },
  fecha_nacimiento: {
    type: 'string',
    description: 'Fecha YYYY-MM-DD; cadena vacía si no se informa.',
    example: '1990-04-12',
  },
};

module.exports = {
  openapi: '3.0.3',

  info: {
    title: 'Rentar - API REST del Gateway',
    version: '1.0.0',
    description:
      'Documentación de las rutas REST implementadas en el API Gateway. ' +
      'Las operaciones se comunican con los servicios mediante gRPC. ' +
      'Los códigos HTTP documentados corresponden a los controladores actuales.',
  },

  servers: [
    { url: '/', description: 'Servidor que expone Swagger UI' },
  ],

  tags: [
    {
      name: 'Autenticacion',
      description: 'Inicio de sesion y obtencion de token Bearer.',
    },
    {
      name: 'Clientes',
      description: 'Alta, consulta, modificación y baja lógica de clientes.',
    },
    {
      name: 'Vehículos',
      description: 'Alta, consulta, modificación, baja y cambio de estado.',
    },
    {
      name: 'Reservas',
      description: 'Creación y cancelación de reservas.',
    },
  ],

  paths: {
    '/api/auth/login': {
      post: operation('Autenticacion', 'Iniciar sesion', 200, ref('LoginResponse'), {
        body: 'LoginRequest',
        errors: { 400: 'Usuario o contrasena no informados.', 401: 'Credenciales invalidas.' },
      }),
    },
    '/api/clientes': {
      get: operation(
        'Clientes', 'Listar clientes', 200, list('Cliente'),
        {
          params: [activosParam],
          errors: { 500: 'Error interno al consultar clientes.' },
        },
      ),

      post: operation(
        'Clientes', 'Crear cliente', 201, ref('Cliente'),
        {
          body: 'ClienteInput',
          errors: {
            400: 'Datos inválidos u otro error de creación.',
            409: 'Documento o correo electrónico ya registrado.',
          },
        },
      ),
    },

    '/api/clientes/{id}': {
      get: operation(
        'Clientes', 'Consultar cliente por ID', 200, ref('Cliente'),
        {
          params: [idParam],
          errors: {
            404: 'Cliente no encontrado.',
            500: 'Error interno al consultar el cliente.',
          },
        },
      ),

      put: operation(
        'Clientes', 'Actualizar cliente', 200, ref('Cliente'),
        {
          params: [idParam],
          body: 'ClienteInput',
          errors: { 400: 'No se pudo actualizar el cliente.' },
          description:
            'Actualiza los datos de un cliente existente. ' +
            'El controlador actual responde 400 ante errores de actualización.',
        },
      ),

      delete: operation(
        'Clientes', 'Dar de baja un cliente', 200, ref('Cliente'),
        {
          params: [idParam],
          errors: { 500: 'Error al desactivar el cliente.' },
          description:
            'Baja lógica: el cliente permanece registrado con activo=false.',
        },
      ),
    },

    '/api/vehiculos': {
      get: operation(
        'Vehículos', 'Listar vehículos', 200, list('Vehiculo'),
        {
          params: [activosParam],
          errors: { 500: 'Error interno al consultar vehículos.' },
        },
      ),

      post: operation(
        'Vehículos', 'Crear vehículo', 201, ref('Vehiculo'),
        {
          body: 'VehiculoCrearInput',
          errors: {
            400: 'Datos inválidos.',
            409: 'La patente ya está registrada.',
          },
          description:
            'Crea un vehículo activo y DISPONIBLE. ' +
            'La verificación policial agregada en Hito 3 es asincrónica.',
        },
      ),
    },

    '/api/vehiculos/patente/{patente}': {
      get: operation(
        'Vehículos', 'Consultar vehículo por patente',
        200, ref('Vehiculo'),
        {
          params: [{
            name: 'patente',
            in: 'path',
            required: true,
            description: 'Patente del vehículo.',
            schema: { type: 'string' },
            example: 'ABC123',
          }],
          errors: {
            404: 'Vehículo no encontrado.',
            500: 'Error interno al consultar el vehículo.',
          },
        },
      ),
    },

    '/api/vehiculos/solicitudes-externas': {
      post: operation('VehÃ­culos', 'Solicitar incorporacion de vehiculo propio', 202, ref('SolicitudVehiculoExterno'), {
        body: 'SolicitudVehiculoExternoInput',
        errors: { 400: 'Datos invalidos para la solicitud.' },
        description: 'CLIENTE usa el clienteId de su sesion. ADMIN puede indicar cliente_id.',
      }),
    },

    '/api/vehiculos/solicitudes-externas/{id}': {
      get: operation('VehÃ­culos', 'Consultar solicitud de incorporacion', 200, ref('SolicitudVehiculoExterno'), {
        params: [idParam],
        errors: { 404: 'Solicitud no encontrada.' },
      }),
    },

    '/api/vehiculos/{id}': {
      get: operation(
        'Vehículos', 'Consultar vehículo por ID',
        200, ref('Vehiculo'),
        {
          params: [idParam],
          errors: {
            404: 'Vehículo no encontrado.',
            500: 'Error interno al consultar el vehículo.',
          },
        },
      ),

      put: operation(
        'Vehículos', 'Actualizar vehículo',
        200, ref('Vehiculo'),
        {
          params: [idParam],
          body: 'VehiculoActualizarInput',
          errors: { 400: 'No se pudo actualizar el vehículo.' },
          description:
            'Permite modificar los datos del vehículo, excepto la patente.',
        },
      ),

      delete: operation(
        'Vehículos', 'Dar de baja un vehículo',
        200, ref('Vehiculo'),
        {
          params: [idParam],
          errors: { 500: 'Error al desactivar el vehículo.' },
          description:
            'Baja lógica: el vehículo permanece registrado con activo=false.',
        },
      ),
    },

    '/api/vehiculos/{id}/estado': {
      patch: operation(
        'Vehículos', 'Modificar estado operativo del vehículo',
        200, ref('Vehiculo'),
        {
          params: [idParam],
          body: 'ActualizarEstadoInput',
          errors: {
            400: 'Estado inválido u otro error.',
            409: 'Operación no permitida por el estado actual.',
          },
        },
      ),
    },

    '/api/reservas': {
      post: operation(
        'Reservas', 'Crear reserva', 201, ref('Reserva'),
        {
          body: 'ReservaCrearInput',
          errors: {
            400: 'Cliente inactivo, vehículo no disponible, ' +
                 'fechas inválidas, solapamiento u otro error.',
          },
          description:
            'El Gateway verifica cliente y vehículo, obtiene el ' +
            'precio diario y solicita la creación de la reserva. ' +
            'El precio_diario no se envía en la petición REST.',
        },
      ),
    },

    '/api/reservas/{id}/cancelar': {
      patch: operation(
        'Reservas', 'Cancelar reserva', 200, ref('Reserva'),
        {
          params: [idParam],
          errors: {
            400: 'Error al cancelar la reserva.',
            409: 'La reserva no puede cancelarse en su estado actual.',
          },
          description:
            'Cancela la reserva mediante una baja lógica, ' +
            'sin eliminarla físicamente.',
        },
      ),
    },
  },

  components: {
    securitySchemes: {
      bearerAuth: { type: 'http', scheme: 'bearer', bearerFormat: 'JWT' },
    },
    schemas: {
      LoginRequest: {
        type: 'object', required: ['username', 'password'],
        properties: { username: { type: 'string', example: 'cliente' }, password: { type: 'string', format: 'password', example: 'C1234!' } },
      },
      LoginResponse: {
        type: 'object',
        properties: {
          token: { type: 'string', description: 'JWT para enviar como Bearer token.' },
          usuario: { type: 'object', properties: { username: { type: 'string' }, rol: { type: 'string', enum: ['ADMIN', 'CLIENTE'] }, clienteId: { type: 'integer', nullable: true } } },
        },
      },
      Error: {
        type: 'object',
        properties: {
          error: {
            type: 'string',
            example: 'Descripción del error',
          },
        },
      },

      ClienteInput: {
        type: 'object',
        required: ['documento', 'nombre', 'apellido', 'email'],
        properties: customerFields,
      },

      Cliente: {
        type: 'object',
        properties: {
          id: {
            type: 'string',
            description: 'Identificador int64 serializado como texto por gRPC.',
            example: '1',
          },
          ...customerFields,
          activo: { type: 'boolean', example: true },
        },
      },

      VehiculoCrearInput: {
        type: 'object',
        required: [
          'patente', 'marca', 'modelo', 'anio', 'tipo', 'precio_diario',
        ],
        properties: {
          patente: { type: 'string', example: 'ABC123' },
          ...vehicleFields,
        },
      },

      VehiculoActualizarInput: {
        type: 'object',
        required: [
          'marca', 'modelo', 'anio', 'tipo', 'precio_diario',
        ],
        properties: vehicleFields,
      },

      ActualizarEstadoInput: {
        type: 'object',
        required: ['estado'],
        properties: {
          estado: {
            type: 'string',
            enum: ['DISPONIBLE', 'RESERVADO', 'EN_ALQUILER'],
            example: 'DISPONIBLE',
          },
        },
      },

      Vehiculo: {
        type: 'object',
        properties: {
          id: {
            type: 'string',
            description: 'Identificador int64 serializado como texto por gRPC.',
            example: '15',
          },
          patente: { type: 'string', example: 'ABC123' },
          ...vehicleFields,
          estado: {
            type: 'string',
            enum: ['DISPONIBLE', 'RESERVADO', 'EN_ALQUILER'],
            example: 'DISPONIBLE',
          },
          activo: { type: 'boolean', example: true },
        },
      },

      SolicitudVehiculoExternoInput: {
        type: 'object',
        required: ['documento', 'patente', 'marca', 'modelo', 'anio', 'tipo', 'precio_diario'],
        properties: {
          cliente_id: { type: 'integer', format: 'int64', description: 'Solo ADMIN puede informarlo.' },
          documento: { type: 'string', example: '30111222' },
          patente: { type: 'string', example: 'NUE123' },
          ...vehicleFields,
        },
      },

      SolicitudVehiculoExterno: {
        type: 'object',
        properties: {
          id: { type: 'string', example: '1' },
          cliente_id: { type: 'string', example: '1' },
          patente: { type: 'string', example: 'NUE123' },
          estado: { type: 'string', enum: ['PENDIENTE', 'INCORPORADA', 'RECHAZADA'] },
          resultado_patente: { type: 'string', enum: ['VALIDADO', 'RECHAZADO', ''] },
          resultado_titularidad: { type: 'string', enum: ['VALIDADO', 'RECHAZADO', ''] },
          vehiculo_id: { type: 'string', nullable: true },
        },
      },

      ReservaCrearInput: {
        type: 'object',
        required: [
          'cliente_id', 'vehiculo_id', 'fecha_inicio', 'fecha_fin',
        ],
        properties: {
          cliente_id: {
            type: 'integer', format: 'int64', example: 1,
          },
          vehiculo_id: {
            type: 'integer', format: 'int64', example: 15,
          },
          fecha_inicio: {
            type: 'string',
            description: 'ISO-8601 sin zona horaria: YYYY-MM-DDTHH:mm:ss.',
            example: '2026-11-05T10:00:00',
          },
          fecha_fin: {
            type: 'string',
            description: 'ISO-8601 sin zona horaria: YYYY-MM-DDTHH:mm:ss.',
            example: '2026-11-08T10:00:00',
          },
        },
      },

      Reserva: {
        type: 'object',
        properties: {
          id: { type: 'string', example: '150' },
          cliente_id: { type: 'string', example: '1' },
          vehiculo_id: { type: 'string', example: '15' },
          fecha_inicio: {
            type: 'string',
            description: 'ISO-8601 sin zona horaria.',
            example: '2026-11-05T10:00:00',
          },
          fecha_fin: {
            type: 'string',
            description: 'ISO-8601 sin zona horaria.',
            example: '2026-11-08T10:00:00',
          },
          precio_diario: {
            type: 'number', format: 'double', example: 45000,
          },
          cantidad_dias: { type: 'integer', example: 3 },
          importe_total: {
            type: 'number', format: 'double', example: 135000,
          },
          estado: {
            type: 'string',
            enum: ['CONFIRMADA', 'CANCELADA', 'FINALIZADA'],
            example: 'CONFIRMADA',
          },
          fecha_alta: {
            type: 'string',
            description: 'ISO-8601 sin zona horaria.',
          },
          fecha_cancelacion: {
            type: 'string',
            description: 'ISO-8601 sin zona horaria; vacío si no se canceló.',
            example: '',
          },
          mantenimiento: {
            type: 'string',
            enum: [
              'MAINTENANCE_STATUS_UNSPECIFIED',
              'PENDIENTE',
              'REALIZADO',
              'NO_REALIZADO',
            ],
            description: 'Estado incorporado para mantenimiento en Hito 3.',
            example: 'PENDIENTE',
          },
        },
      },
    },
  },
};
