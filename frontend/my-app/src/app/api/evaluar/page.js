"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

const API_BASE = "http://localhost:8080/api/evaluar";

function AvatarPlaceholder({ size = "md" }) {
  const dimensions = size === "sm" ? "w-10 h-10" : "w-14 h-14";
  return (
    <div
      className={`${dimensions} border border-gray-500 bg-white flex items-center justify-center shrink-0`}
    >
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

export default function EvaluarCandidatosPage() {
  const router = useRouter();

  // Control de vista: 1 = Búsqueda Candidatos, 2 = Evaluar Candidatos
  const [paso, setPaso] = useState(1);

  // --- ESTADO VISTA 1: BÚSQUEDA DE CANDIDATOS ---
  const [nombre, setNombre] = useState("");
  const [apellido, setApellido] = useState("");
  const [nroCandidato, setNroCandidato] = useState("");

  const [candidatosDisponibles, setCandidatosDisponibles] = useState([]);
  const [seleccionadosParaAgregar, setSeleccionadosParaAgregar] = useState([]); // IDs marcados con "+ add"
  const [candidatosAEvaluar, setCandidatosAEvaluar] = useState([]);

  // Cartel rojo flotante (string o { titulo, items })
  const [errorBanner, setErrorBanner] = useState(null);

  // --- ESTADO VISTA 2: EVALUAR CANDIDATOS ---
  const [puestos, setPuestos] = useState([]);
  const [codPuestoSeleccionado, setCodPuestoSeleccionado] = useState("");
  const [competencias, setCompetencias] = useState([]);
  const [candidatosConClave, setCandidatosConClave] = useState([]);
  const [puestoConfirmado, setPuestoConfirmado] = useState(false);

  // Modal rojo con botón "Aceptar" (competencias no evaluables)
  const [modalErrorCompetencias, setModalErrorCompetencias] = useState("");

  // Modal verde de exportación a Excel
  const [mostrarModalExcel, setMostrarModalExcel] = useState(false);
  const [cargando, setCargando] = useState(false);

  // =========================================================
  // HANDLERS VISTA 1: BÚSQUEDA CANDIDATOS
  // =========================================================

  const handleFiltrar = async (e) => {
    e.preventDefault();
    setErrorBanner(null);

    const params = new URLSearchParams();
    if (nombre.trim() !== "") params.append("nombre", nombre.trim());
    if (apellido.trim() !== "") params.append("apellido", apellido.trim());
    if (nroCandidato.trim() !== "") params.append("nroCandidato", nroCandidato.trim());

    try {
      setCargando(true);
      const res = await fetch(`${API_BASE}?${params.toString()}`);

      if (!res.ok) {
        // Excepción del backend: "Debe ingresar al menos un argumento"
        const mensaje = await res.text();
        setErrorBanner({ texto: mensaje });
        return;
      }

      const data = await res.json();
      setCandidatosDisponibles(data);
      setSeleccionadosParaAgregar([]);
    } catch {
      setErrorBanner({ texto: "Error de conexión con el servidor." });
    } finally {
      setCargando(false);
    }
  };

  const toggleSeleccionDisponible = (id) => {
    setErrorBanner(null);
    setSeleccionadosParaAgregar((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  };

  const handleAgregarCandidatos = async () => {
    setErrorBanner(null);

    // Validación propia del front (según wireframe)
    if (seleccionadosParaAgregar.length === 0) {
      setErrorBanner({
        texto: "Debe seleccionar al menos un candidato para agregar",
      });
      return;
    }

    try {
      setCargando(true);
      const res = await fetch(`${API_BASE}/validar`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(seleccionadosParaAgregar),
      });

      if (res.status === 409) {
        // Backend devuelve List<Integer> con IDs que tienen cuestionario Activo o En Proceso
        const idsInvalidos = await res.json();
        const nombresInvalidos = candidatosDisponibles
          .filter((c) => idsInvalidos.includes(c.nroCandidato))
          .map((c) => `${c.nombre} ${c.apellido}`);

        setErrorBanner({
          texto:
            "Los siguientes candidatos ya poseen cuestionarios activos o en proceso y no pueden ser evaluados nuevamente:",
          items: nombresInvalidos,
        });
        return;
      }

      if (!res.ok) {
        const mensaje = await res.text();
        setErrorBanner({ texto: mensaje });
        return;
      }

      // Todos válidos: los pasamos a "Candidatos a Evaluar" sin duplicados
      const nuevosCandidatos = candidatosDisponibles.filter(
        (c) =>
          seleccionadosParaAgregar.includes(c.nroCandidato) &&
          !candidatosAEvaluar.some((ya) => ya.nroCandidato === c.nroCandidato)
      );

      setCandidatosAEvaluar((prev) => [...prev, ...nuevosCandidatos]);
      setSeleccionadosParaAgregar([]);
    } catch {
      setErrorBanner({ texto: "Error al validar los candidatos seleccionados." });
    } finally {
      setCargando(false);
    }
  };

  const handleQuitarCandidato = (id) => {
    setErrorBanner(null);
    setCandidatosAEvaluar((prev) => prev.filter((c) => c.nroCandidato !== id));
  };

  const handleSiguientePaso1 = async () => {
    setErrorBanner(null);

    if (candidatosAEvaluar.length === 0) {
      setErrorBanner({
        texto: "Debe seleccionar al menos un candidato para evaluar.",
      });
      return;
    }

    try {
      setCargando(true);
      const res = await fetch(`${API_BASE}/puestos`);
      if (!res.ok) {
        const mensaje = await res.text();
        setErrorBanner({ texto: mensaje });
        return;
      }

      const listaPuestos = await res.json();
      setPuestos(listaPuestos);
      setCodPuestoSeleccionado("");
      setCompetencias([]);
      setCandidatosConClave([]);
      setPuestoConfirmado(false);
      setPaso(2);
    } catch {
      setErrorBanner({ texto: "No se pudieron cargar los puestos disponibles." });
    } finally {
      setCargando(false);
    }
  };

  // =========================================================
  // HANDLERS VISTA 2: EVALUAR CANDIDATOS
  // =========================================================

  const handleCambioPuesto = (nuevoCodigo) => {
    setErrorBanner(null);
    setCodPuestoSeleccionado(nuevoCodigo);
    setCompetencias([]);
    setCandidatosConClave([]);
    setPuestoConfirmado(false);
  };

  const handleSiguientePuesto = async () => {
    setErrorBanner(null);

    // Validación propia del front (según wireframe)
    if (!codPuestoSeleccionado) {
      setErrorBanner({
        texto: "Debe seleccionar una funcion/puesto a evaluar.",
      });
      return;
    }

    try {
      setCargando(true);

      // 1. Validar factores evaluables y traer competencias + ponderaciones
      const resComp = await fetch(
        `${API_BASE}/puestos/${encodeURIComponent(codPuestoSeleccionado)}/competencias`
      );

      if (!resComp.ok) {
        // Excepción del backend: "Las siguientes competencias no pueden ser evaluadas: ..."
        // o "El puesto seleccionado no tiene competencias asociadas"
        const mensaje = await resComp.text();
        setModalErrorCompetencias(mensaje);
        return;
      }

      const listaCompetencias = await resComp.json();

      // 2. Obtener datos completos de candidatos y generar claves de 8 caracteres
      const ids = candidatosAEvaluar.map((c) => c.nroCandidato);
      const resClaves = await fetch(`${API_BASE}/candidatos-a-evaluar`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(ids),
      });

      if (!resClaves.ok) {
        const mensaje = await resClaves.text();
        setErrorBanner({ texto: mensaje });
        return;
      }

      const listaCandidatosClave = await resClaves.json();

      setCompetencias(listaCompetencias);
      setCandidatosConClave(listaCandidatosClave);
      setPuestoConfirmado(true);
    } catch {
      setErrorBanner({ texto: "Error al procesar la evaluación del puesto." });
    } finally {
      setCargando(false);
    }
  };

  const handleFinalizar = async () => {
    setErrorBanner(null);

    if (!puestoConfirmado || candidatosConClave.length === 0) {
      setErrorBanner({
        texto: "Debe seleccionar una funcion/puesto a evaluar.",
      });
      return;
    }

    try {
      setCargando(true);
      const res = await fetch(
        `${API_BASE}/finalizar?codPuesto=${encodeURIComponent(codPuestoSeleccionado)}`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(candidatosConClave),
        }
      );

      if (!res.ok) {
        const mensaje = await res.text();
        setErrorBanner({ texto: mensaje });
        return;
      }

      // Paso 13 completado: mostrar modal verde de exportación a Excel
      setMostrarModalExcel(true);
    } catch {
      setErrorBanner({ texto: "Error al guardar la evaluación." });
    } finally {
      setCargando(false);
    }
  };

  const exportarExcelLocal = () => {
    const puestoObj = puestos.find((p) => p.codigo === codPuestoSeleccionado);
    const nombreFuncion = (puestoObj?.nombre || "Funcion")
      .replace(/[\\/:*?"<>|]/g, "")
      .trim();

    const ahora = new Date();
    const pad = (n) => String(n).padStart(2, "0");
    const fechaStr = [
      ahora.getFullYear(),
      pad(ahora.getMonth() + 1),
      pad(ahora.getDate()),
      pad(ahora.getHours()),
      pad(ahora.getMinutes()),
    ].join("-");

    const nombreArchivo = `${fechaStr}-${nombreFuncion}.xls`;

    const filasHtml = candidatosConClave
      .map(
        (c) => `
        <tr>
          <td>${c.nombre ?? ""}</td>
          <td>${c.apellido ?? ""}</td>
          <td>${c.tipoDocumento ?? ""}</td>
          <td>${c.nroDocumento ?? ""}</td>
          <td>${c.claveGenerada ?? ""}</td>
        </tr>`
      )
      .join("");

    const tablaHtml = `
      <html xmlns:o="urn:schemas-microsoft-com:office:office"
            xmlns:x="urn:schemas-microsoft-com:office:excel"
            xmlns="http://www.w3.org/TR/REC-html40">
        <head><meta charset="UTF-8" /></head>
        <body>
          <table border="1">
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Apellido</th>
                <th>Tipo documento</th>
                <th>Nrodocumento</th>
                <th>Clave de ingreso</th>
              </tr>
            </thead>
            <tbody>${filasHtml}</tbody>
          </table>
        </body>
      </html>`;

    const blob = new Blob(["\ufeff", tablaHtml], {
      type: "application/vnd.ms-excel;charset=utf-8;",
    });
    const url = URL.createObjectURL(blob);
    const enlace = document.createElement("a");
    enlace.href = url;
    enlace.download = nombreArchivo;
    document.body.appendChild(enlace);
    enlace.click();
    document.body.removeChild(enlace);
    URL.revokeObjectURL(url);
  };

  const handleSiExcel = () => {
    exportarExcelLocal();
    setMostrarModalExcel(false);
    router.push("/");
  };

  const handleNoExcel = () => {
    setMostrarModalExcel(false);
    router.push("/");
  };

  // =========================================================
  // RENDER
  // =========================================================

  return (
    <div className="min-h-screen bg-white text-black flex flex-col justify-between p-6 md:p-10 relative">
      {/* CABECERA COMÚN */}
      <div>
        <header className="flex items-center justify-between border-b border-gray-400 pb-3 mb-8">
          <h1 className="text-3xl md:text-4xl font-normal tracking-tight">
            {paso === 1 ? "Busqueda Candidatos" : "Evaluar Candidatos"}
          </h1>

          <div className="flex items-center gap-3 text-right">
            <div className="text-xs text-gray-700 leading-tight">
              <p>&lt;Nombre usuario&gt;</p>
              <p>[rol]</p>
            </div>
            <AvatarPlaceholder size="sm" />
          </div>
        </header>

        {/* CARTEL ROJO DE ERROR (FLOTANTE, SIN BOTÓN ACEPTAR) */}
        {errorBanner && (
          <div
            onClick={() => setErrorBanner(null)}
            title="Clic para cerrar"
            className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 z-40 bg-rose-500 text-black px-7 py-4 rounded-2xl shadow-xl max-w-md w-11/12 cursor-pointer"
          >
            <p className="text-sm font-medium">{errorBanner.texto}</p>
            {errorBanner.items && errorBanner.items.length > 0 && (
              <ul className="list-disc list-inside mt-2 text-sm">
                {errorBanner.items.map((item, idx) => (
                  <li key={idx}>{item}</li>
                ))}
              </ul>
            )}
          </div>
        )}

        {/* MODAL ROJO CON BOTÓN "ACEPTAR" (COMPETENCIAS NO EVALUABLES - FLUJO 9.A) */}
        {modalErrorCompetencias && (
          <div className="fixed inset-0 bg-black/20 flex items-center justify-center z-50 p-4">
            <div className="bg-rose-500 text-black px-8 py-6 rounded-2xl shadow-2xl max-w-md w-full flex flex-col items-center text-center gap-5">
              <p className="text-sm font-medium">{modalErrorCompetencias}</p>
              <button
                type="button"
                onClick={() => {
                  setModalErrorCompetencias("");
                  router.push("/");
                }}
                className="bg-black text-white px-8 py-2 rounded text-sm font-medium hover:bg-gray-800 transition"
              >
                Aceptar
              </button>
            </div>
          </div>
        )}

        {/* MODAL VERDE DE EXPORTACIÓN A EXCEL (PASOS 13-17) */}
        {mostrarModalExcel && (
          <div className="fixed inset-0 bg-black/20 flex items-center justify-center z-50 p-4">
            <div className="bg-emerald-400 text-black px-8 py-6 rounded-2xl shadow-2xl max-w-md w-full flex flex-col items-center text-center gap-5">
              <p className="text-sm font-medium">
                Desea exportar los datos de los candidatos con sus claves en excel.
              </p>
              <div className="flex gap-6">
                <button
                  type="button"
                  onClick={handleNoExcel}
                  className="bg-black text-white px-10 py-2 rounded text-sm font-medium hover:bg-gray-800 transition"
                >
                  No
                </button>
                <button
                  type="button"
                  onClick={handleSiExcel}
                  className="bg-black text-white px-10 py-2 rounded text-sm font-medium hover:bg-gray-800 transition"
                >
                  Si
                </button>
              </div>
            </div>
          </div>
        )}

        {/* ===================================================== */}
        {/* VISTA 1: BÚSQUEDA DE CANDIDATOS                       */}
        {/* ===================================================== */}
        {paso === 1 && (
          <main className="space-y-6">
            {/* Formulario de Filtros */}
            <form onSubmit={handleFiltrar} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div>
                  <label className="block text-sm mb-1">Nombre:</label>
                  <input
                    type="text"
                    value={nombre}
                    onChange={(e) => setNombre(e.target.value)}
                    placeholder="ej: jake"
                    className="w-full border border-gray-400 rounded px-3 py-1.5 text-sm focus:outline-none focus:border-black"
                  />
                </div>

                <div>
                  <label className="block text-sm mb-1">Apellido:</label>
                  <input
                    type="text"
                    value={apellido}
                    onChange={(e) => setApellido(e.target.value)}
                    placeholder="ej: peralta"
                    className="w-full border border-gray-400 rounded px-3 py-1.5 text-sm focus:outline-none focus:border-black"
                  />
                </div>

                <div>
                  <label className="block text-sm mb-1">Numero de Candidato:</label>
                  <input
                    type="number"
                    value={nroCandidato}
                    onChange={(e) => setNroCandidato(e.target.value)}
                    placeholder="ej: 99"
                    className="w-full border border-gray-400 rounded px-3 py-1.5 text-sm focus:outline-none focus:border-black"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={cargando}
                className="bg-black text-white px-8 py-2 rounded text-sm font-medium hover:bg-gray-800 transition disabled:opacity-50"
              >
                Filtrar
              </button>
            </form>

            {/* Contenedor: Candidatos Disponibles */}
            <section className="space-y-2">
              <h2 className="text-sm font-semibold">Candidatos Disponibles:</h2>
              <div className="bg-gray-200 rounded-xl p-4 min-h-[155px] overflow-x-auto flex items-center gap-6">
                {candidatosDisponibles.length === 0 ? (
                  <p className="text-xs text-gray-500 mx-auto">
                    Sin resultados para mostrar.
                  </p>
                ) : (
                  candidatosDisponibles.map((cand) => {
                    const marcado = seleccionadosParaAgregar.includes(cand.nroCandidato);
                    return (
                      <div
                        key={cand.nroCandidato}
                        className={`flex flex-col items-center text-center min-w-[110px] p-2 rounded-lg transition ${
                          marcado ? "bg-gray-300 ring-2 ring-black" : ""
                        }`}
                      >
                        <AvatarPlaceholder />
                        <p className="text-xs font-medium mt-2 leading-tight">
                          {cand.nombre} {cand.apellido}
                        </p>
                        <p className="text-xs text-gray-700 mb-2">
                          {cand.nroCandidato}
                        </p>
                        <button
                          type="button"
                          onClick={() => toggleSeleccionDisponible(cand.nroCandidato)}
                          className={`px-3 py-1 rounded text-xs font-medium transition ${
                            marcado
                              ? "bg-emerald-700 text-white"
                              : "bg-black text-white hover:bg-gray-800"
                          }`}
                        >
                          {marcado ? "✓ add" : "+ add"}
                        </button>
                      </div>
                    );
                  })
                )}
              </div>

              <div className="flex justify-end pt-2">
                <button
                  type="button"
                  onClick={handleAgregarCandidatos}
                  disabled={cargando}
                  className="bg-black text-white px-6 py-2 rounded text-sm font-medium hover:bg-gray-800 transition disabled:opacity-50"
                >
                  + Agregar
                </button>
              </div>
            </section>

            {/* Contenedor: Candidatos a Evaluar */}
            <section className="space-y-2">
              <h2 className="text-sm font-semibold">Candidatos a Evaluar:</h2>
              <div className="bg-gray-200 rounded-xl p-4 min-h-[155px] overflow-x-auto flex items-center gap-6">
                {candidatosAEvaluar.length === 0 ? (
                  <p className="text-xs text-gray-500 mx-auto">
                    Aún no hay candidatos agregados para evaluar.
                  </p>
                ) : (
                  candidatosAEvaluar.map((cand) => (
                    <div
                      key={cand.nroCandidato}
                      className="flex flex-col items-center text-center min-w-[110px] p-2"
                    >
                      <AvatarPlaceholder />
                      <p className="text-xs font-medium mt-2 leading-tight">
                        {cand.nombre} {cand.apellido}
                      </p>
                      <p className="text-xs text-gray-700 mb-2">
                        {cand.nroCandidato}
                      </p>
                      <button
                        type="button"
                        onClick={() => handleQuitarCandidato(cand.nroCandidato)}
                        className="bg-black text-white px-3 py-1 rounded text-xs font-medium hover:bg-gray-800 transition"
                      >
                        - Quitar
                      </button>
                    </div>
                  ))
                )}
              </div>
            </section>
          </main>
        )}

        {/* ===================================================== */}
        {/* VISTA 2: EVALUAR CANDIDATOS                           */}
        {/* ===================================================== */}
        {paso === 2 && (
          <main className="space-y-8">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8 items-start">
              {/* Columna Izquierda: Selector de Puesto + Siguiente */}
              <div className="space-y-5">
                <div>
                  <label className="block text-sm mb-1.5">
                    Funcion/Puesto a evaluar:
                  </label>
                  <select
                    value={codPuestoSeleccionado}
                    onChange={(e) => handleCambioPuesto(e.target.value)}
                    className="w-full max-w-md border border-gray-400 rounded px-3 py-2 text-sm bg-white focus:outline-none focus:border-black"
                  >
                    <option value="">
                      Empresa: [Nombre de la empresa] Funcion:[Nombre]
                    </option>
                    {puestos.map((p) => (
                      <option key={p.codigo} value={p.codigo}>
                        Empresa: {p.empresa} Funcion: {p.nombre}
                      </option>
                    ))}
                  </select>
                </div>

                <button
                  type="button"
                  onClick={handleSiguientePuesto}
                  disabled={cargando}
                  className="bg-black text-white px-8 py-2 rounded text-sm font-medium hover:bg-gray-800 transition disabled:opacity-50"
                >
                  Siguiente
                </button>
              </div>

              {/* Columna Derecha: Tabla Competencia / Ponderación */}
              <div className="border border-gray-500 max-h-56 overflow-y-auto">
                <table className="w-full text-left border-collapse text-sm">
                  <thead className="bg-gray-100 sticky top-0">
                    <tr>
                      <th className="border-b border-r border-gray-500 px-3 py-1.5 font-semibold">
                        Competencia
                      </th>
                      <th className="border-b border-gray-500 px-3 py-1.5 font-semibold">
                        Ponderacion
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {competencias.length === 0 ? (
                      <tr>
                        <td
                          colSpan={2}
                          className="px-3 py-6 text-center text-xs text-gray-500"
                        >
                          Seleccione una función y presione Siguiente para ver sus competencias.
                        </td>
                      </tr>
                    ) : (
                      competencias.map((comp, idx) => (
                        <tr key={idx}>
                          <td className="border-b border-r border-gray-400 px-3 py-1">
                            {comp.nombre ?? comp.competencia}
                          </td>
                          <td className="border-b border-gray-400 px-3 py-1">
                            {comp.ponderacion ?? comp.ponderacionMinima}%
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Grilla de Candidatos con Clave de Ingreso (aparece tras confirmar Puesto) */}
            {puestoConfirmado && (
              <section className="space-y-2">
                <h2 className="text-base font-semibold">Candidatos a evaluar:</h2>
                <div className="bg-gray-200 rounded-xl p-5 max-h-80 overflow-y-auto grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 lg:grid-cols-8 gap-5">
                  {candidatosConClave.map((cand) => (
                    <div
                      key={cand.nro_candidato}
                      className="flex flex-col items-center text-center text-xs leading-snug"
                    >
                      <AvatarPlaceholder />
                      <p className="font-semibold mt-2">
                        {cand.nombre} {cand.apellido}
                      </p>
                      <p className="text-gray-700">{cand.tipoDocumento}</p>
                      <p className="text-gray-700">{cand.nroDocumento}</p>
                      <p className="font-mono font-bold mt-0.5 text-black">
                        {cand.claveGenerada}
                      </p>
                    </div>
                  ))}
                </div>
              </section>
            )}
          </main>
        )}
      </div>

      {/* PIE DE PÁGINA CON BOTONES DE NAVEGACIÓN */}
      <footer className="flex items-center justify-between pt-8 mt-6">
        <button
          type="button"
          onClick={() => {
            setErrorBanner(null);
            if (paso === 2) {
              setPaso(1);
            } else {
              router.push("/");
            }
          }}
          className="bg-black text-white px-8 py-2 rounded text-sm font-medium hover:bg-gray-800 transition"
        >
          Volver
        </button>

        {paso === 1 ? (
          <button
            type="button"
            onClick={handleSiguientePaso1}
            disabled={cargando}
            className="bg-black text-white px-8 py-2 rounded text-sm font-medium hover:bg-gray-800 transition disabled:opacity-50"
          >
            Siguiente
          </button>
        ) : (
          <div className="flex gap-4">
            <button
              type="button"
              onClick={() => router.push("/")}
              className="bg-black text-white px-8 py-2 rounded text-sm font-medium hover:bg-gray-800 transition"
            >
              Cancelar
            </button>
            <button
              type="button"
              onClick={handleFinalizar}
              disabled={!puestoConfirmado || cargando}
              className="bg-black text-white px-8 py-2 rounded text-sm font-medium hover:bg-gray-800 transition disabled:opacity-40"
            >
              Finalizar
            </button>
          </div>
        )}
      </footer>
    </div>
  );
}