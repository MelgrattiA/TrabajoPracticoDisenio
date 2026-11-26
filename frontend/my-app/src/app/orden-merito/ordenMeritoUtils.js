/**
 * Utilidades de formateo y normalización de entrada para el CU-28 (Orden de Mérito).
 */

/**
 * Convierte un texto literal a mayúsculas respetando el idioma español (tildes y Ñ)
 * y truncando a la longitud máxima permitida.
 */
export function normalizarMayusculas(valor, maxLength) {
  const texto = (valor ?? "").toLocaleUpperCase("es-AR");
  return typeof maxLength === "number" ? texto.slice(0, maxLength) : texto;
}

/**
 * Calcula el nuevo valor y la nueva posición del cursor al pegar texto en un input
 * garantizando conversión a mayúsculas y respeto de maxLength.
 */
export function aplicarPegadoMayusculas(valorActual, textoPegado, inicio, fin, maxLength) {
  const prefijo = valorActual.slice(0, inicio);
  const sufijo = valorActual.slice(fin);
  const pegadoMayus = (textoPegado ?? "").toLocaleUpperCase("es-AR");
  const combinado = (prefijo + pegadoMayus + sufijo).toLocaleUpperCase("es-AR");
  const resultado = typeof maxLength === "number" ? combinado.slice(0, maxLength) : combinado;
  const cursor = Math.min(inicio + pegadoMayus.length, resultado.length);
  return { resultado, cursor };
}

/**
 * Formatea una fecha ISO (LocalDateTime de Spring Boot) a DD/MM/AAAA HH:mm.
 */
export function formatearFechaHora(fechaIso) {
  if (!fechaIso) return "-";
  const fecha = new Date(fechaIso);
  if (Number.isNaN(fecha.getTime())) return String(fechaIso);

  const pad = (n) => String(n).padStart(2, "0");
  return `${pad(fecha.getDate())}/${pad(fecha.getMonth() + 1)}/${fecha.getFullYear()} ${pad(fecha.getHours())}:${pad(fecha.getMinutes())}`;
}

/**
 * Extrae el mensaje de error legible desde una respuesta HTTP de Spring Boot
 * (soporta ProblemDetail RFC 7807 con campo `detail` o texto plano).
 */
export async function extraerMensajeError(response) {
  const texto = await response.text();
  if (!texto) {
    return `Error del servidor (HTTP ${response.status}).`;
  }
  try {
    const json = JSON.parse(texto);
    if (json && typeof json.detail === "string" && json.detail.trim() !== "") {
      return json.detail;
    }
    if (json && typeof json.message === "string" && json.message.trim() !== "") {
      return json.message;
    }
  } catch {
    // La respuesta no es JSON; se devuelve el texto plano.
  }
  return texto;
}
