"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import {
  aplicarPegadoMayusculas,
  extraerMensajeError,
  formatearFechaHora,
  normalizarMayusculas,
} from "./ordenMeritoUtils";

const API_BASE = "http://localhost:8080/api/orden-merito";

function AvatarPlaceholder() {
  return (
    <div className="w-10 h-10 border border-gray-500 bg-white flex items-center justify-center shrink-0">
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.5"
        className="w-3/4 h-3/4 text-gray-500"
      >
        <circle cx="12" cy="8" r="4" />
        <path d="M4 20c0-4 3.6-7 8-7s8 3 8 7" />
      </svg>
    </div>
  );
}

function Cabecera({ titulo }) {
  return (
    <header className="flex items-center justify-between border-b border-gray-400 pb-3 mb-6">
      <h1 className="text-3xl md:text-4xl font-normal tracking-tight">
        {titulo}
      </h1>

      <div className="flex items-center gap-3 text-right">
        <div className="text-xs text-gray-700 leading-tight">
          <p>&lt;Nombre usuario&gt;</p>
          <p>[rol]</p>
        </div>
        <AvatarPlaceholder />
      </div>
    </header>
  );
}

function TablaCandidatosCompletados({ titulo, filas, mostrarPosicion }) {
  const colSpan = mostrarPosicion ? 9 : 8;

  return (
    <section className="space-y-1.5">
      <h3 className="text-sm font-medium">{titulo}</h3>
      <div className="border border-gray-500 max-h-56 overflow-auto print:max-h-none print:overflow-visible">
        <table className="w-full text-left border-collapse text-xs print:text-[10px]">
          <thead className="bg-gray-100 sticky top-0">
            <tr>
              {mostrarPosicion && (
                <th className="border-b border-r border-gray-500 px-3 py-1.5 print:px-1.5 print:py-1 font-semibold">
                  Pos.
                </th>
              )}
              <th className="border-b border-r border-gray-500 px-3 py-1.5 print:px-1.5 print:py-1 font-semibold">
                Tipo de documento
              </th>
              <th className="border-b border-r border-gray-500 px-3 py-1.5 print:px-1.5 print:py-1 font-semibold">
                Numero de documento
              </th>
              <th className="border-b border-r border-gray-500 px-3 py-1.5 print:px-1.5 print:py-1 font-semibold">
                Apellido
              </th>
              <th className="border-b border-r border-gray-500 px-3 py-1.5 print:px-1.5 print:py-1 font-semibold">
                Nombre
              </th>
              <th className="border-b border-r border-gray-500 px-3 py-1.5 print:px-1.5 print:py-1 font-semibold">
                Puntaje obtenido
              </th>
              <th className="border-b border-r border-gray-500 px-3 py-1.5 print:px-1.5 print:py-1 font-semibold">
                Fecha inicio
              </th>
              <th className="border-b border-r border-gray-500 px-3 py-1.5 print:px-1.5 print:py-1 font-semibold">
                Fecha de fin
              </th>
              <th className="border-b border-gray-500 px-3 py-1.5 print:px-1.5 print:py-1 font-semibold">
                Cantidad de accesos
              </th>
            </tr>
          </thead>
          <tbody>
            {filas.length === 0 ? (
              <tr>
                <td
                  colSpan={colSpan}
                  className="px-3 py-4 print:px-1.5 print:py-2 text-center text-gray-500"
                >
                  Sin candidatos en esta categoría.
                </td>
              </tr>
            ) : (
              filas.map((item) => (
                <tr key={item.claveCuestionario} className="hover:bg-gray-50">
                  {mostrarPosicion && (
                    <td className="border-b border-r border-gray-300 px-3 py-1.5 print:px-1.5 print:py-1 font-semibold">
                      {item.posicion}
                    </td>
                  )}
                  <td className="border-b border-r border-gray-300 px-3 py-1.5 print:px-1.5 print:py-1">
                    {item.tipoDocumento ?? "-"}
                  </td>
                  <td className="border-b border-r border-gray-300 px-3 py-1.5 print:px-1.5 print:py-1">
                    {item.nroDocumento ?? "-"}
                  </td>
                  <td className="border-b border-r border-gray-300 px-3 py-1.5 print:px-1.5 print:py-1">
                    {item.apellido ?? "-"}
                  </td>
                  <td className="border-b border-r border-gray-300 px-3 py-1.5 print:px-1.5 print:py-1">
                    {item.nombre ?? "-"}
                  </td>
                  <td className="border-b border-r border-gray-300 px-3 py-1.5 print:px-1.5 print:py-1 font-medium">
                    {item.puntajeObtenido ?? "-"}
                  </td>
                  <td className="border-b border-r border-gray-300 px-3 py-1.5 print:px-1.5 print:py-1">
                    {formatearFechaHora(item.fechaInicio)}
                  </td>
                  <td className="border-b border-r border-gray-300 px-3 py-1.5 print:px-1.5 print:py-1">
                    {formatearFechaHora(item.fechaFinalizacion)}
                  </td>
                  <td className="border-b border-gray-300 px-3 py-1.5 print:px-1.5 print:py-1">
                    {item.cantidadAccesos ?? 0}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function TablaCandidatosSinCompletar({ sinCompletarPorEstado }) {
  const filasAgrupadas = Object.entries(sinCompletarPorEstado || {}).flatMap(
    ([estado, cuestionarios]) =>
      (cuestionarios || []).map((c) => ({
        ...c,
        estadoNombre: estado,
      }))
  );

  return (
    <section className="space-y-1.5">
      <h3 className="text-sm font-medium">Candidatos Sin completar:</h3>
      <div className="border border-gray-500 max-h-56 overflow-auto print:max-h-none print:overflow-visible">
        <table className="w-full text-left border-collapse text-xs print:text-[10px]">
          <thead className="bg-gray-100 sticky top-0">
            <tr>
              <th className="border-b border-r border-gray-500 px-3 py-1.5 print:px-1.5 print:py-1 font-semibold">
                Tipo de documento
              </th>
              <th className="border-b border-r border-gray-500 px-3 py-1.5 print:px-1.5 print:py-1 font-semibold">
                Numero de documento
              </th>
              <th className="border-b border-r border-gray-500 px-3 py-1.5 print:px-1.5 print:py-1 font-semibold">
                Apellido
              </th>
              <th className="border-b border-r border-gray-500 px-3 py-1.5 print:px-1.5 print:py-1 font-semibold">
                Nombre
              </th>
              <th className="border-b border-r border-gray-500 px-3 py-1.5 print:px-1.5 print:py-1 font-semibold">
                Estado
              </th>
              <th className="border-b border-r border-gray-500 px-3 py-1.5 print:px-1.5 print:py-1 font-semibold">
                Fecha inicio
              </th>
              <th className="border-b border-r border-gray-500 px-3 py-1.5 print:px-1.5 print:py-1 font-semibold">
                Fecha de fin
              </th>
              <th className="border-b border-gray-500 px-3 py-1.5 print:px-1.5 print:py-1 font-semibold">
                Cantidad de accesos
              </th>
            </tr>
          </thead>
          <tbody>
            {filasAgrupadas.length === 0 ? (
              <tr>
                <td colSpan={8} className="px-3 py-4 print:px-1.5 print:py-2 text-center text-gray-500">
                  Sin candidatos en esta categoría.
                </td>
              </tr>
            ) : (
              filasAgrupadas.map((item) => (
                <tr key={item.claveCuestionario} className="hover:bg-gray-50">
                  <td className="border-b border-r border-gray-300 px-3 py-1.5 print:px-1.5 print:py-1">
                    {item.tipoDocumento ?? "-"}
                  </td>
                  <td className="border-b border-r border-gray-300 px-3 py-1.5 print:px-1.5 print:py-1">
                    {item.nroDocumento ?? "-"}
                  </td>
                  <td className="border-b border-r border-gray-300 px-3 py-1.5 print:px-1.5 print:py-1">
                    {item.apellido ?? "-"}
                  </td>
                  <td className="border-b border-r border-gray-300 px-3 py-1.5 print:px-1.5 print:py-1">
                    {item.nombre ?? "-"}
                  </td>
                  <td className="border-b border-r border-gray-300 px-3 py-1.5 print:px-1.5 print:py-1">
                    {item.estadoNombre}
                  </td>
                  <td className="border-b border-r border-gray-300 px-3 py-1.5 print:px-1.5 print:py-1">
                    {formatearFechaHora(item.fechaInicio)}
                  </td>
                  <td className="border-b border-r border-gray-300 px-3 py-1.5 print:px-1.5 print:py-1">
                    {formatearFechaHora(item.fechaUltimoIngreso)}
                  </td>
                  <td className="border-b border-gray-300 px-3 py-1.5 print:px-1.5 print:py-1">
                    {item.cantidadAccesos ?? 0}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}

export default function OrdenMeritoPage() {
  const router = useRouter();

  // Control de vista: "busqueda" | "reporte"
  const [vista, setVista] = useState("busqueda");

  // Filtros de búsqueda (siempre en mayúsculas)
  const [codigo, setCodigo] = useState("");
  const [nombre, setNombre] = useState("");
  const [empresa, setEmpresa] = useState("");

  // Resultados y estado de UI
  const [puestos, setPuestos] = useState([]);
  const [busquedaRealizada, setBusquedaRealizada] = useState(false);
  const [puestoSeleccionado, setPuestoSeleccionado] = useState(null);
  const [reporte, setReporte] = useState(null);

  const [cargando, setCargando] = useState(false);
  const [mensajeError, setMensajeError] = useState("");

  const crearHandlerCambioMayusculas = (setter, maxLength) => (e) => {
    const input = e.target;
    const inicio = input.selectionStart ?? input.value.length;
    const fin = input.selectionEnd ?? input.value.length;
    const valorMayus = normalizarMayusculas(input.value, maxLength);
    setter(valorMayus);
    setMensajeError("");

    requestAnimationFrame(() => {
      if (document.activeElement === input) {
        const pos = Math.min(inicio, valorMayus.length);
        const posFin = Math.min(fin, valorMayus.length);
        input.setSelectionRange(pos, posFin);
      }
    });
  };

  const crearHandlerPegadoMayusculas = (valorActual, setter, maxLength) => (e) => {
    e.preventDefault();
    const input = e.target;
    const inicio = input.selectionStart ?? valorActual.length;
    const fin = input.selectionEnd ?? valorActual.length;
    const textoPegado = e.clipboardData?.getData("text") ?? "";

    const { resultado, cursor } = aplicarPegadoMayusculas(
      valorActual,
      textoPegado,
      inicio,
      fin,
      maxLength
    );

    setter(resultado);
    setMensajeError("");

    requestAnimationFrame(() => {
      if (document.activeElement === input) {
        input.setSelectionRange(cursor, cursor);
      }
    });
  };

  const handleBuscar = async (e) => {
    e.preventDefault();
    if (cargando) return;
    setMensajeError("");

    const codigoNorm = normalizarMayusculas(codigo.trim(), 50);
    const nombreNorm = normalizarMayusculas(nombre.trim(), 100);
    const empresaNorm = normalizarMayusculas(empresa.trim(), 100);

    if (!codigoNorm && !nombreNorm && !empresaNorm) {
      setMensajeError("Ingrese al menos un campo para poder realizar la busqueda.");
      return;
    }

    const params = new URLSearchParams();
    if (codigoNorm) params.append("codigo", codigoNorm);
    if (nombreNorm) params.append("nombre", nombreNorm);
    if (empresaNorm) params.append("empresa", empresaNorm);

    try {
      setCargando(true);
      const res = await fetch(`${API_BASE}/puestos?${params.toString()}`);

      if (!res.ok) {
        const msg = await extraerMensajeError(res);
        setMensajeError(msg);
        return;
      }

      const data = await res.json();
      setPuestos(data);
      setBusquedaRealizada(true);
    } catch {
      setMensajeError("No se pudo conectar con el servidor backend.");
    } finally {
      setCargando(false);
    }
  };

  const handleCancelarBusqueda = () => {
    setCodigo("");
    setNombre("");
    setEmpresa("");
    setPuestos([]);
    setBusquedaRealizada(false);
    setMensajeError("");
  };

  const handleEmitirOrdenMerito = async (puesto) => {
    if (cargando) return;
    setMensajeError("");

    // Flujo alternativo 6.A: el puesto no posee evaluaciones
    if (!puesto.candidatos || puesto.candidatos === 0) {
      setMensajeError("El puesto no posee evaluaciones.");
      return;
    }

    try {
      setCargando(true);
      const res = await fetch(
        `${API_BASE}/puestos/${encodeURIComponent(puesto.codigo)}/reporte`
      );

      if (!res.ok) {
        const msg = await extraerMensajeError(res);
        setMensajeError(msg);
        return;
      }

      const data = await res.json();
      const sinCompletarTotal = Object.values(
        data.cuestionariosSinCompletarPorEstado || {}
      ).reduce((acc, lista) => acc + (lista?.length || 0), 0);

      if (
        (data.aprobados?.length || 0) === 0 &&
        (data.desaprobados?.length || 0) === 0 &&
        sinCompletarTotal === 0
      ) {
        setMensajeError("El puesto no posee evaluaciones.");
        return;
      }

      setPuestoSeleccionado(puesto);
      setReporte(data);
      setVista("reporte");
    } catch {
      setMensajeError("No se pudo conectar con el servidor backend.");
    } finally {
      setCargando(false);
    }
  };

  const handleVolver = () => {
    setMensajeError("");
    if (vista === "reporte") {
      setVista("busqueda");
      setReporte(null);
      setPuestoSeleccionado(null);
    } else {
      router.push("/");
    }
  };

  const handleImprimir = () => {
    window.print();
  };

  return (
    <div className="min-h-screen bg-white text-black flex flex-col justify-between p-6 md:p-10 print:p-0">
      <div>
        <Cabecera
          titulo={
            vista === "busqueda" ? "Sistema de reclutamiento" : "Orden de Merito"
          }
        />

        {vista === "busqueda" ? (
          <main className="max-w-5xl mx-auto space-y-6">
            <h2 className="text-center text-lg underline underline-offset-4 font-normal">
              Emitir Orden de Merito
            </h2>

            {/* Formulario de Criterios de Búsqueda */}
            <form
              onSubmit={handleBuscar}
              className="border border-gray-400 rounded-lg p-6 max-w-2xl mx-auto space-y-4"
            >
              <h3 className="text-sm font-semibold mb-2">
                Criterios de Búsqueda de Puesto
              </h3>

              <div className="grid grid-cols-1 sm:grid-cols-[160px_1fr] items-center gap-2 sm:gap-4">
                <label htmlFor="codigoPuesto" className="text-sm">
                  Código del Puesto:
                </label>
                <input
                  id="codigoPuesto"
                  type="text"
                  maxLength={50}
                  value={codigo}
                  onChange={crearHandlerCambioMayusculas(setCodigo, 50)}
                  onPaste={crearHandlerPegadoMayusculas(codigo, setCodigo, 50)}
                  placeholder="Ej: C001, C002.."
                  className="border border-gray-400 rounded px-3 py-1.5 text-sm focus:outline-none focus:border-black"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-[160px_1fr] items-center gap-2 sm:gap-4">
                <label htmlFor="nombrePuesto" className="text-sm">
                  Nombre del Puesto:
                </label>
                <input
                  id="nombrePuesto"
                  type="text"
                  maxLength={100}
                  value={nombre}
                  onChange={crearHandlerCambioMayusculas(setNombre, 100)}
                  onPaste={crearHandlerPegadoMayusculas(nombre, setNombre, 100)}
                  placeholder="Ej: ANALISTA DE DATOS, DESARROLLADOR.."
                  className="border border-gray-400 rounded px-3 py-1.5 text-sm focus:outline-none focus:border-black"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-[160px_1fr] items-center gap-2 sm:gap-4">
                <label htmlFor="empresaPuesto" className="text-sm">
                  Empresa:
                </label>
                <input
                  id="empresaPuesto"
                  type="text"
                  maxLength={100}
                  value={empresa}
                  onChange={crearHandlerCambioMayusculas(setEmpresa, 100)}
                  onPaste={crearHandlerPegadoMayusculas(empresa, setEmpresa, 100)}
                  placeholder="Ej: GLOBALTECH, SOLUTIONS INC.."
                  className="border border-gray-400 rounded px-3 py-1.5 text-sm focus:outline-none focus:border-black"
                />
              </div>

              <div className="flex justify-center gap-4 pt-2">
                <button
                  type="submit"
                  disabled={cargando}
                  className="bg-black text-white px-8 py-2 rounded text-sm font-medium hover:bg-gray-800 transition cursor-pointer disabled:opacity-50"
                >
                  {cargando ? "Buscando..." : "Buscar"}
                </button>
                <button
                  type="button"
                  onClick={handleCancelarBusqueda}
                  disabled={cargando}
                  className="bg-black text-white px-8 py-2 rounded text-sm font-medium hover:bg-gray-800 transition cursor-pointer disabled:opacity-50"
                >
                  Cancelar
                </button>
              </div>
            </form>

            {/* Cartel Rojo de Error */}
            {mensajeError && (
              <div className="flex justify-center">
                <div
                  role="alert"
                  onClick={() => setMensajeError("")}
                  title="Clic para cerrar"
                  className="bg-rose-500 text-black font-medium text-sm px-6 py-4 rounded-xl shadow-lg max-w-md text-center cursor-pointer"
                >
                  {mensajeError}
                </div>
              </div>
            )}

            {/* Grilla de Puestos Encontrados */}
            {busquedaRealizada && (
              <section className="space-y-2">
                <h3 className="text-sm font-semibold">Puestos Encontrados:</h3>
                <div className="border border-gray-500 max-h-72 overflow-y-auto">
                  <table className="w-full text-left border-collapse text-xs">
                    <thead className="bg-gray-100 sticky top-0">
                      <tr>
                        <th className="border-b border-r border-gray-500 px-3 py-2 font-semibold">
                          CODIGO
                        </th>
                        <th className="border-b border-r border-gray-500 px-3 py-2 font-semibold">
                          NOMBRE PUESTO
                        </th>
                        <th className="border-b border-r border-gray-500 px-3 py-2 font-semibold">
                          EMPRESA
                        </th>
                        <th className="border-b border-r border-gray-500 px-3 py-2 font-semibold">
                          CANDIDATOS
                        </th>
                        <th className="border-b border-r border-gray-500 px-3 py-2 font-semibold">
                          EVALUACIONES COMPLETADAS
                        </th>
                        <th className="border-b border-gray-500 px-3 py-2 font-semibold">
                          ACCION
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {puestos.length === 0 ? (
                        <tr>
                          <td
                            colSpan={6}
                            className="px-3 py-4 text-center text-gray-600"
                          >
                            No se encontraron puestos que coincidan con los criterios ingresados.
                          </td>
                        </tr>
                      ) : (
                        puestos.map((puesto) => (
                          <tr key={puesto.codigo} className="hover:bg-gray-50">
                            <td className="border-b border-r border-gray-300 px-3 py-2 font-mono">
                              {puesto.codigo}
                            </td>
                            <td className="border-b border-r border-gray-300 px-3 py-2">
                              {puesto.nombrePuesto}
                            </td>
                            <td className="border-b border-r border-gray-300 px-3 py-2">
                              {puesto.empresa}
                            </td>
                            <td className="border-b border-r border-gray-300 px-3 py-2">
                              {puesto.candidatos}
                            </td>
                            <td className="border-b border-r border-gray-300 px-3 py-2">
                              {puesto.evaluacionesCompletas}
                            </td>
                            <td className="border-b border-gray-300 px-3 py-2">
                              <button
                                type="button"
                                onClick={() => handleEmitirOrdenMerito(puesto)}
                                disabled={cargando}
                                className="text-blue-700 underline hover:text-blue-900 cursor-pointer disabled:opacity-50"
                              >
                                Emitir orden de merito
                              </button>
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </section>
            )}
          </main>
        ) : (
          <main className="max-w-6xl mx-auto space-y-6">
            <h2 className="text-center text-lg font-normal">
              {puestoSeleccionado
                ? `${puestoSeleccionado.nombrePuesto} — ${puestoSeleccionado.empresa} (${puestoSeleccionado.codigo})`
                : "Nombre Puesto / Funcion Evaluada"}
            </h2>

            <TablaCandidatosCompletados
              titulo="Candidatos Aprobados:"
              filas={reporte?.aprobados || []}
              mostrarPosicion={true}
            />

            <TablaCandidatosCompletados
              titulo="Candidatos Desaprobados:"
              filas={reporte?.desaprobados || []}
              mostrarPosicion={false}
            />

            <TablaCandidatosSinCompletar
              sinCompletarPorEstado={
                reporte?.cuestionariosSinCompletarPorEstado || {}
              }
            />
          </main>
        )}
      </div>

      {/* PIE DE PÁGINA CON BOTONES DE NAVEGACIÓN / IMPRESIÓN */}
      <footer className="max-w-6xl w-full mx-auto flex items-center justify-between pt-8 mt-6 print:hidden">
        <button
          type="button"
          onClick={handleVolver}
          className="bg-black text-white px-8 py-2 rounded text-sm font-medium hover:bg-gray-800 transition cursor-pointer"
        >
          Volver
        </button>

        {vista === "reporte" && (
          <button
            type="button"
            onClick={handleImprimir}
            className="bg-black text-white px-8 py-2 rounded text-sm font-medium hover:bg-gray-800 transition cursor-pointer"
          >
            Imprimir
          </button>
        )}
      </footer>
    </div>
  );
}
