const API_BASE = "http://localhost:8080/api/preguntas";

export class PreguntasApiError extends Error {
  constructor(status, payload) {
    super(payload?.detail || payload?.mensaje || `Error HTTP ${status}`);
    this.name = "PreguntasApiError";
    this.status = status;
    this.payload = payload;
  }
}

async function solicitar(ruta, opciones = {}) {
  const respuesta = await fetch(`${API_BASE}${ruta}`, opciones);
  const texto = await respuesta.text();
  let cuerpo = texto;

  if (texto) {
    try {
      cuerpo = JSON.parse(texto);
    } catch {
      cuerpo = texto;
    }
  }

  if (!respuesta.ok) {
    throw new PreguntasApiError(respuesta.status, cuerpo);
  }

  return cuerpo;
}

export function obtenerCompetencias(signal) {
  return solicitar("/competencias", { signal });
}

export function obtenerFactores(codigoCompetencia, signal) {
  return solicitar(
    `/competencias/${encodeURIComponent(codigoCompetencia)}/factores`,
    { signal }
  );
}

export function obtenerOpcionesRespuesta(signal) {
  return solicitar("/opciones-respuesta", { signal });
}

export function obtenerItems(idOpcionRespuesta, signal) {
  return solicitar(
    `/opciones-respuesta/${encodeURIComponent(idOpcionRespuesta)}/items`,
    { signal }
  );
}

export function crearPregunta(solicitud) {
  return solicitar("", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(solicitud),
  });
}

export function restaurarPregunta(codigo) {
  return solicitar(`/${encodeURIComponent(codigo)}/restaurar`, {
    method: "POST",
  });
}
