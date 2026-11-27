"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";

const API_BASE = "http://localhost:8080/api/cuestionarios";

function paginasVisibles(actual, total) {
  if (total <= 5) {
    return Array.from({ length: total }, (_, indice) => indice + 1);
  }
  if (actual <= 3) {
    return [1, 2, 3, "...", total - 1, total];
  }
  if (actual >= total - 2) {
    return [1, "...", total - 2, total - 1, total];
  }
  return [1, "...", actual - 1, actual, actual + 1, "...", total - 1, total];
}

export default function ResponderCuestionarioPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const clave = useMemo(
    () => (searchParams.get("clave") || "").trim(),
    [searchParams]
  );
  const [bloque, setBloque] = useState(null);
  const [selecciones, setSelecciones] = useState({});
  const [preguntasFaltantes, setPreguntasFaltantes] = useState([]);
  const [mensaje, setMensaje] = useState("");
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [completo, setCompleto] = useState(false);

  useEffect(() => {
    let cancelado = false;

    const cargarBloque = async () => {
      if (!clave) {
        setMensaje("Falta la clave del cuestionario.");
        setCargando(false);
        return;
      }

      try {
        const response = await fetch(
          `${API_BASE}/${encodeURIComponent(clave)}/bloque-pendiente`
        );
        const payload = await response.json();
        if (!response.ok) {
          throw new Error(payload?.detail || "No se pudo cargar el bloque.");
        }
        if (!cancelado) {
          setBloque(payload);
        }
      } catch (error) {
        if (!cancelado) {
          setMensaje(error.message || "No se pudo cargar el cuestionario.");
        }
      } finally {
        if (!cancelado) {
          setCargando(false);
        }
      }
    };

    cargarBloque();
    return () => {
      cancelado = true;
    };
  }, [clave]);

  const responder = async (event) => {
    event.preventDefault();
    if (!bloque || guardando) return;

    setMensaje("");
    const sinRespuesta = bloque.preguntas.filter(
      (pregunta) => !selecciones[pregunta.idPreguntaBloque]
    );
    if (sinRespuesta.length > 0) {
      setPreguntasFaltantes(
        sinRespuesta.map((pregunta) => pregunta.ordenEnBloque)
      );
      return;
    }

    setPreguntasFaltantes([]);
    const respuestas = bloque.preguntas.map((pregunta) => ({
      idPreguntaBloque: pregunta.idPreguntaBloque,
      idItemOpcion: selecciones[pregunta.idPreguntaBloque],
    }));

    try {
      setGuardando(true);
      const response = await fetch(
        `${API_BASE}/${encodeURIComponent(clave)}/bloques/${bloque.idBloquePregunta}/respuestas`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ respuestas }),
        }
      );
      const payload = await response.json();
      if (!response.ok) {
        throw new Error(payload?.detail || "No se pudieron guardar las respuestas.");
      }
      if (!payload.guardado) {
        const idsFaltantes = new Set(payload.preguntasFaltantes || []);
        setPreguntasFaltantes(
          bloque.preguntas
            .filter((pregunta) => idsFaltantes.has(pregunta.idPreguntaBloque))
            .map((pregunta) => pregunta.ordenEnBloque)
        );
        return;
      }
      if (payload.cuestionarioCompleto) {
        setCompleto(true);
        setBloque(null);
        return;
      }

      setBloque(payload.siguienteBloque);
      setSelecciones({});
    } catch (error) {
      setMensaje(error.message || "No se pudieron guardar las respuestas.");
    } finally {
      setGuardando(false);
    }
  };

  const esUltimoBloque =
    bloque && bloque.nroOrden >= bloque.cantidadBloques;

  return (
    <main className="min-h-screen bg-white px-5 py-3 text-black md:px-10">
      <header className="flex items-center justify-between border-b border-gray-500 pb-2">
        <h1 className="text-4xl font-semibold md:text-5xl">Cuestionario</h1>
        <div className="flex items-center gap-3 text-right text-xs text-gray-700">
          <div>
            <p>&lt;Nombre usuario&gt;</p>
            <p>[rol]</p>
          </div>
          <div
            aria-hidden="true"
            className="flex h-12 w-12 items-center justify-center border border-gray-500 text-3xl text-gray-500"
          >
            ♙
          </div>
        </div>
      </header>

      <section className="mx-auto flex min-h-[calc(100vh-6rem)] max-w-5xl flex-col py-8 md:py-12">
        {cargando ? (
          <p role="status" className="m-auto">
            Cargando preguntas...
          </p>
        ) : mensaje && !bloque ? (
          <p role="alert" className="m-auto rounded bg-rose-100 p-5 text-rose-900">
            {mensaje}
          </p>
        ) : completo ? null : bloque ? (
          <>
            <form onSubmit={responder} className="flex flex-1 flex-col">
              <div className="space-y-6">
                {bloque.preguntas.map((pregunta) => (
                  <section key={pregunta.idPreguntaBloque}>
                    <h2 className="mb-1 text-sm font-semibold">
                      Pregunta Nro-{pregunta.ordenEnBloque}:
                    </h2>
                    <div className="overflow-hidden rounded border border-gray-400">
                      <table className="w-full border-collapse text-left text-sm">
                        <thead className="bg-gray-100">
                          <tr>
                            <th className="w-1/2 border-b border-r border-gray-400 px-3 py-2 font-semibold">
                              {pregunta.nombrePregunta}: {pregunta.pregunta}
                            </th>
                            <th className="border-b border-gray-400 px-3 py-2 font-semibold">
                              Opciones
                            </th>
                          </tr>
                        </thead>
                        <tbody>
                          {pregunta.opciones.map((opcion, indice) => (
                            <tr key={opcion.idItemOpcion}>
                              <td className="border-b border-r border-gray-300 px-3 py-2 last:border-b-0">
                                <label className="flex cursor-pointer items-center gap-2">
                                  <input
                                    type="radio"
                                    name={`pregunta-${pregunta.idPreguntaBloque}`}
                                    value={opcion.idItemOpcion}
                                    checked={
                                      Number(
                                        selecciones[pregunta.idPreguntaBloque]
                                      ) === opcion.idItemOpcion
                                    }
                                    onChange={() =>
                                      setSelecciones((actuales) => ({
                                        ...actuales,
                                        [pregunta.idPreguntaBloque]:
                                          opcion.idItemOpcion,
                                      }))
                                    }
                                  />
                                  <span>Opción {indice + 1}</span>
                                </label>
                              </td>
                              <td className="border-b border-gray-300 px-3 py-2 last:border-b-0">
                                {opcion.descripcion}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </section>
                ))}
              </div>

              <p className="mt-auto pt-8 text-sm">
                Solo se puede seleccionar una opción
              </p>

              {preguntasFaltantes.length > 0 && (
                <div
                  role="alert"
                  className="my-4 self-center rounded-xl bg-rose-500 px-7 py-4 text-left text-lg text-black shadow-lg"
                >
                  <p>Las siguientes preguntas no han sido respondidas:</p>
                  <ul className="list-disc pl-6">
                    {preguntasFaltantes.map((nroPregunta) => (
                      <li key={nroPregunta}>
                        Número de Bloque: {bloque.nroOrden} Pregunta: {nroPregunta}
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {mensaje && (
                <p role="alert" className="my-4 rounded bg-rose-100 p-4 text-rose-900">
                  {mensaje}
                </p>
              )}

              <nav
                aria-label="Progreso del cuestionario"
                className="flex items-center justify-center gap-2 py-5"
              >
                {paginasVisibles(
                  bloque.nroOrden,
                  bloque.cantidadBloques
                ).map((pagina, indice) =>
                  pagina === "..." ? (
                    <span key={`ellipsis-${indice}`} className="px-2">
                      ...
                    </span>
                  ) : (
                    <span
                      key={pagina}
                      aria-current={pagina === bloque.nroOrden ? "step" : undefined}
                      className={`min-w-9 rounded px-3 py-2 text-center ${
                        pagina === bloque.nroOrden
                          ? "bg-gray-800 text-white"
                          : "text-gray-800"
                      }`}
                    >
                      {pagina}
                    </span>
                  )
                )}
              </nav>

              <footer className="flex items-center justify-between pt-2">
                <button
                  type="button"
                  onClick={() => router.push("/")}
                  className="rounded bg-black px-8 py-2 text-xl font-semibold text-white hover:bg-gray-800"
                >
                  Salir
                </button>
                <button
                  type="submit"
                  disabled={guardando}
                  className="rounded bg-black px-5 py-2 text-xl font-semibold text-white hover:bg-gray-800 disabled:opacity-50"
                >
                  {guardando
                    ? "Guardando..."
                    : esUltimoBloque
                      ? "Finalizar"
                      : "Siguiente"}
                </button>
              </footer>
            </form>
          </>
        ) : (
          <p role="alert" className="m-auto rounded bg-rose-100 p-5 text-rose-900">
            No hay un bloque pendiente para este cuestionario.
          </p>
        )}
      </section>

      {completo && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="cuestionario-completado"
          className="fixed inset-0 z-10 flex items-center justify-center bg-black/20 p-5"
        >
          <div className="max-w-sm rounded-2xl bg-green-400 px-8 py-7 text-center text-xl shadow-xl">
            <p id="cuestionario-completado">
              El cuestionario ha sido completado con éxito.
            </p>
            <button
              type="button"
              onClick={() => router.push("/")}
              className="mt-5 rounded bg-black px-6 py-2 text-white hover:bg-gray-800"
            >
              Aceptar
            </button>
          </div>
        </div>
      )}
    </main>
  );
}
