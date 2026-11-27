"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";

import {
  crearPregunta,
  obtenerCompetencias,
  obtenerFactores,
  obtenerItems,
  obtenerOpcionesRespuesta,
  restaurarPregunta,
} from "../../../../lib/preguntasApi";

const SUMA_REQUERIDA_CENTAVOS = 1000n;

const FORMULARIO_INICIAL = {
  codigoCompetencia: "",
  codigoFactor: "",
  nombre: "",
  pregunta: "",
  descripcion: "",
  idOpcionRespuesta: "",
};

function convertirACentavos(valor) {
  if (!/^\d+(?:[.,]\d{1,2})?$/.test(valor)) {
    return null;
  }

  const [unidades, decimales = ""] = valor.replace(",", ".").split(".");
  return (
    BigInt(unidades) * 100n +
    BigInt((decimales || "0").padEnd(2, "0"))
  );
}

function formatearCentavos(centavos) {
  return `${centavos / 100n}.${(centavos % 100n).toString().padStart(2, "0")}`;
}

function mensajeDeError(error) {
  if (error?.name === "AbortError") {
    return "";
  }
  if (error?.payload && typeof error.payload === "object") {
    return error.payload.detail || error.payload.mensaje || error.message;
  }
  if (error instanceof TypeError) {
    return "No se pudo conectar con el servidor backend.";
  }
  return error?.message || "Ocurrió un error inesperado.";
}

function mapearErroresServidor(errores, items) {
  const resultado = {};

  for (const [campo, mensajes] of Object.entries(errores || {})) {
    const texto = Array.isArray(mensajes)
      ? mensajes.filter(Boolean).join(" ")
      : String(mensajes);
    const coincidencia = /^ponderaciones\[(\d+)\]/.exec(campo);

    if (coincidencia) {
      const item = items[Number(coincidencia[1])];
      const clave = item
        ? `ponderaciones.${item.idItemOpcion}`
        : "ponderaciones";
      resultado[clave] = texto;
    } else {
      resultado[campo] = texto;
    }
  }

  return resultado;
}

function validarFormulario(formulario, items, ponderaciones) {
  const errores = {};

  if (!formulario.codigoCompetencia) {
    errores.codigoCompetencia = "Seleccione una competencia.";
  }
  if (!formulario.codigoFactor) {
    errores.codigoFactor = "Seleccione un factor.";
  }
  if (!formulario.nombre.trim()) {
    errores.nombre = "El nombre es obligatorio.";
  } else if (formulario.nombre.trim().length > 100) {
    errores.nombre = "El nombre no puede superar los 100 caracteres.";
  }
  if (!formulario.pregunta.trim()) {
    errores.pregunta = "El texto de la pregunta es obligatorio.";
  } else if (formulario.pregunta.trim().length > 150) {
    errores.pregunta = "La pregunta no puede superar los 150 caracteres.";
  }
  if (formulario.descripcion.trim().length > 500) {
    errores.descripcion =
      "La descripción no puede superar los 500 caracteres.";
  }
  if (!formulario.idOpcionRespuesta) {
    errores.idOpcionRespuesta = "Seleccione una opción de respuesta.";
  }
  if (items.length === 0) {
    errores.ponderaciones =
      "La opción de respuesta debe tener ítems configurados.";
    return errores;
  }

  let sumaCentavos = 0n;
  let todasValidas = true;

  for (const item of items) {
    const clave = `ponderaciones.${item.idItemOpcion}`;
    const valor = ponderaciones[item.idItemOpcion] ?? "";
    const centavos = convertirACentavos(valor);

    if (valor.trim() === "") {
      errores[clave] = "Ingrese una ponderación.";
      todasValidas = false;
    } else if (centavos === null) {
      errores[clave] =
        "Ingrese un valor no negativo con un máximo de dos decimales.";
      todasValidas = false;
    } else if (centavos > SUMA_REQUERIDA_CENTAVOS) {
      errores[clave] = "Cada ponderación debe estar entre 0 y 10.";
      todasValidas = false;
    } else {
      sumaCentavos += centavos;
    }
  }

  if (
    todasValidas &&
    sumaCentavos !== SUMA_REQUERIDA_CENTAVOS
  ) {
    errores.ponderaciones =
      "La suma de las ponderaciones debe ser exactamente 10.00.";
  }

  return errores;
}

export default function AltaPreguntaPage() {
  const router = useRouter();
  const [formulario, setFormulario] = useState(FORMULARIO_INICIAL);
  const [competencias, setCompetencias] = useState([]);
  const [factores, setFactores] = useState([]);
  const [opcionesRespuesta, setOpcionesRespuesta] = useState([]);
  const [items, setItems] = useState([]);
  const [ponderaciones, setPonderaciones] = useState({});
  const [cargandoCompetencias, setCargandoCompetencias] = useState(true);
  const [cargandoFactores, setCargandoFactores] = useState(false);
  const [cargandoOpciones, setCargandoOpciones] = useState(true);
  const [cargandoItems, setCargandoItems] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [restaurando, setRestaurando] = useState(false);
  const [erroresCampo, setErroresCampo] = useState({});
  const [mensaje, setMensaje] = useState("");
  const [conflicto, setConflicto] = useState(null);
  const [codigoCandidatoSeleccionado, setCodigoCandidatoSeleccionado] =
    useState("");
  const [altaExitosa, setAltaExitosa] = useState(false);
  const [restauracionExitosa, setRestauracionExitosa] = useState(null);
  const envioEnCurso = useRef(false);
  const restauracionEnCurso = useRef(false);
  const controladoresDependientes = useRef({ factores: null, items: null });
  const solicitudesDependientes = useRef({ factores: 0, items: 0 });

  useEffect(() => {
    const controlador = new AbortController();
    let vigente = true;

    obtenerCompetencias(controlador.signal)
      .then((datos) => {
        if (vigente) {
          setCompetencias(datos);
        }
      })
      .catch((error) => {
        if (vigente && error.name !== "AbortError") {
          setMensaje(`No se pudieron cargar las competencias. ${mensajeDeError(error)}`);
        }
      })
      .finally(() => {
        if (vigente) {
          setCargandoCompetencias(false);
        }
      });

    obtenerOpcionesRespuesta(controlador.signal)
      .then((datos) => {
        if (vigente) {
          setOpcionesRespuesta(datos);
        }
      })
      .catch((error) => {
        if (vigente && error.name !== "AbortError") {
          setMensaje(`No se pudieron cargar las opciones. ${mensajeDeError(error)}`);
        }
      })
      .finally(() => {
        if (vigente) {
          setCargandoOpciones(false);
        }
      });

    return () => {
      vigente = false;
      controlador.abort();
    };
  }, []);

  useEffect(() => {
    const controladores = controladoresDependientes.current;
    const solicitudes = solicitudesDependientes.current;

    return () => {
      controladores.factores?.abort();
      controladores.items?.abort();
      solicitudes.factores += 1;
      solicitudes.items += 1;
    };
  }, []);

  const cargarFactores = (codigoCompetencia) => {
    solicitudesDependientes.current.factores += 1;
    const numeroSolicitud = solicitudesDependientes.current.factores;
    controladoresDependientes.current.factores?.abort();
    controladoresDependientes.current.factores = null;
    setFactores([]);

    if (!codigoCompetencia) {
      setCargandoFactores(false);
      return;
    }

    const controlador = new AbortController();
    controladoresDependientes.current.factores = controlador;
    setCargandoFactores(true);

    obtenerFactores(codigoCompetencia, controlador.signal)
      .then((datos) => {
        if (solicitudesDependientes.current.factores === numeroSolicitud) {
          setFactores(datos);
        }
      })
      .catch((error) => {
        if (
          solicitudesDependientes.current.factores === numeroSolicitud &&
          error.name !== "AbortError"
        ) {
          setMensaje(
            `No se pudieron cargar los factores. ${mensajeDeError(error)}`
          );
        }
      })
      .finally(() => {
        if (solicitudesDependientes.current.factores === numeroSolicitud) {
          setCargandoFactores(false);
        }
      });
  };

  const cargarItems = (idOpcionRespuesta) => {
    solicitudesDependientes.current.items += 1;
    const numeroSolicitud = solicitudesDependientes.current.items;
    controladoresDependientes.current.items?.abort();
    controladoresDependientes.current.items = null;
    setItems([]);
    setPonderaciones({});

    if (!idOpcionRespuesta) {
      setCargandoItems(false);
      return;
    }

    const controlador = new AbortController();
    controladoresDependientes.current.items = controlador;
    setCargandoItems(true);

    obtenerItems(idOpcionRespuesta, controlador.signal)
      .then((datos) => {
        if (solicitudesDependientes.current.items === numeroSolicitud) {
          setItems(datos);
        }
      })
      .catch((error) => {
        if (
          solicitudesDependientes.current.items === numeroSolicitud &&
          error.name !== "AbortError"
        ) {
          setMensaje(
            `No se pudieron cargar los ítems. ${mensajeDeError(error)}`
          );
        }
      })
      .finally(() => {
        if (solicitudesDependientes.current.items === numeroSolicitud) {
          setCargandoItems(false);
        }
      });
  };

  const limpiarConflictoSiCambiaClave = (campo) => {
    if (
      conflicto &&
      ["codigoFactor", "nombre", "pregunta"].includes(campo)
    ) {
      setConflicto(null);
      setCodigoCandidatoSeleccionado("");
    }
  };

  const actualizarTexto = (campo, valor) => {
    setFormulario((actual) => ({
      ...actual,
      [campo]: valor.toLocaleUpperCase("es-AR"),
    }));
    setErroresCampo((actuales) => ({ ...actuales, [campo]: undefined }));
    limpiarConflictoSiCambiaClave(campo);
    setMensaje("");
  };

  const actualizarPonderacion = (idItemOpcion, valor) => {
    setPonderaciones((actuales) => ({
      ...actuales,
      [idItemOpcion]: valor,
    }));
    setErroresCampo((actuales) => ({
      ...actuales,
      [`ponderaciones.${idItemOpcion}`]: undefined,
      ponderaciones: undefined,
    }));
    setMensaje("");
  };

  const manejarCambioCompetencia = (evento) => {
    const codigoCompetencia = evento.target.value;
    setFormulario((actual) => ({
      ...actual,
      codigoCompetencia,
      codigoFactor: "",
    }));
    setFactores([]);
    cargarFactores(codigoCompetencia);
    setErroresCampo((actuales) => ({
      ...actuales,
      codigoCompetencia: undefined,
      codigoFactor: undefined,
    }));
    limpiarConflictoSiCambiaClave("codigoFactor");
    setMensaje("");
  };

  const manejarCambioOpcion = (evento) => {
    setFormulario((actual) => ({
      ...actual,
      idOpcionRespuesta: evento.target.value,
    }));
    cargarItems(evento.target.value);
    setErroresCampo((actuales) => ({
      ...actuales,
      idOpcionRespuesta: undefined,
      ponderaciones: undefined,
    }));
    setMensaje("");
  };

  const manejarErrorEnvio = (error) => {
    if (error.status === 409 && error.payload) {
      setConflicto(error.payload);
      setCodigoCandidatoSeleccionado("");
      setMensaje("");
      return;
    }

    const erroresServidor = error.payload?.errores;
    if (erroresServidor) {
      setErroresCampo(mapearErroresServidor(erroresServidor, items));
    }
    setMensaje(mensajeDeError(error));
  };

  const enviarAlta = async (evento) => {
    evento.preventDefault();
    if (envioEnCurso.current || enviando || restaurando) {
      return;
    }

    const errores = validarFormulario(formulario, items, ponderaciones);
    setErroresCampo(errores);
    setMensaje("");
    if (Object.keys(errores).length > 0) {
      return;
    }

    envioEnCurso.current = true;
    setEnviando(true);
    setConflicto(null);

    const solicitud = {
      codigoCompetencia: Number(formulario.codigoCompetencia),
      codigoFactor: Number(formulario.codigoFactor),
      nombre: formulario.nombre.trim(),
      pregunta: formulario.pregunta.trim(),
      descripcion: formulario.descripcion.trim() || null,
      idOpcionRespuesta: Number(formulario.idOpcionRespuesta),
      ponderaciones: items.map((item) => ({
        idItemOpcion: Number(item.idItemOpcion),
        ponderacion: Number(
          ponderaciones[item.idItemOpcion].replace(",", ".")
        ),
      })),
    };

    try {
      await crearPregunta(solicitud);
      setAltaExitosa(true);
    } catch (error) {
      manejarErrorEnvio(error);
    } finally {
      envioEnCurso.current = false;
      setEnviando(false);
    }
  };

  const restaurarCandidato = async () => {
    if (
      !codigoCandidatoSeleccionado ||
      restauracionEnCurso.current ||
      restaurando ||
      enviando
    ) {
      return;
    }

    restauracionEnCurso.current = true;
    setRestaurando(true);
    setMensaje("");

    try {
      const resultado = await restaurarPregunta(
        Number(codigoCandidatoSeleccionado)
      );
      setConflicto(null);
      setRestauracionExitosa(resultado);
    } catch (error) {
      manejarErrorEnvio(error);
    } finally {
      restauracionEnCurso.current = false;
      setRestaurando(false);
    }
  };

  const reiniciarFormulario = () => {
    solicitudesDependientes.current.factores += 1;
    solicitudesDependientes.current.items += 1;
    controladoresDependientes.current.factores?.abort();
    controladoresDependientes.current.items?.abort();
    controladoresDependientes.current.factores = null;
    controladoresDependientes.current.items = null;
    setCargandoFactores(false);
    setCargandoItems(false);
    setFormulario({ ...FORMULARIO_INICIAL });
    setFactores([]);
    setItems([]);
    setPonderaciones({});
    setErroresCampo({});
    setMensaje("");
    setConflicto(null);
    setCodigoCandidatoSeleccionado("");
    setAltaExitosa(false);
  };

  const sumaCentavos = items.reduce((total, item) => {
    const centavos = convertirACentavos(
      ponderaciones[item.idItemOpcion] ?? ""
    );
    return centavos === null ? total : total + centavos;
  }, 0n);

  const candidatos = conflicto?.restauracion?.candidatos ?? [];
  const bloquearAlta =
    conflicto?.codigo === "PREGUNTA_DUPLICADA_ACTIVA" ||
    cargandoCompetencias ||
    cargandoFactores ||
    cargandoOpciones ||
    cargandoItems ||
    enviando ||
    restaurando;

  return (
    <main className="min-h-screen bg-white p-5 text-black sm:p-8">
      <div className="mx-auto w-full max-w-5xl">
        <header className="mb-8 flex items-center justify-between border-b border-gray-400 pb-3">
          <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">
            Alta de Preguntas
          </h1>
          <div className="flex items-center gap-3 text-right text-xs text-gray-700">
            <div className="leading-tight">
              <p className="font-semibold">&lt;Nombre usuario&gt;</p>
              <p>[rol]</p>
            </div>
            <div
              aria-hidden="true"
              className="flex h-10 w-10 items-center justify-center border border-gray-500 text-gray-500"
            >
              ✕
            </div>
          </div>
        </header>

        {mensaje && (
          <div
            role="alert"
            className="mb-5 rounded-lg bg-rose-100 px-4 py-3 text-sm text-rose-900"
          >
            {mensaje}
          </div>
        )}

        {conflicto && (
          <section
            aria-labelledby="titulo-conflicto"
            className="mb-6 rounded-lg border border-rose-400 bg-rose-50 p-4"
          >
            <h2 id="titulo-conflicto" className="font-semibold text-rose-900">
              {conflicto.mensaje}
            </h2>
            {conflicto.campoCoincidencia && (
              <p className="mt-1 text-sm text-rose-900">
                Coincidencia: {conflicto.campoCoincidencia}
              </p>
            )}

            {candidatos.length > 0 ? (
              <div className="mt-4 space-y-3">
                <p className="text-sm">
                  Seleccione explícitamente la pregunta eliminada que desea
                  restaurar:
                </p>
                {candidatos.map((candidato) => (
                  <label
                    key={candidato.codigo}
                    className="flex cursor-pointer gap-3 rounded border border-gray-300 bg-white p-3"
                  >
                    <input
                      type="radio"
                      name="candidatoRestauracion"
                      value={candidato.codigo}
                      checked={
                        codigoCandidatoSeleccionado ===
                        String(candidato.codigo)
                      }
                      onChange={(evento) =>
                        setCodigoCandidatoSeleccionado(evento.target.value)
                      }
                      disabled={restaurando || enviando}
                      className="mt-1"
                    />
                    <span className="min-w-0 text-sm">
                      <span className="block font-semibold">
                        Código {candidato.codigo}: {candidato.nombre}
                      </span>
                      <span className="block">{candidato.pregunta}</span>
                      <span className="mt-1 block text-gray-600">
                        Factor {candidato.codigoFactor} · Opción:{" "}
                        {candidato.nombreOpcionRespuesta}
                      </span>
                      {candidato.descripcion && (
                        <span className="block text-gray-600">
                          {candidato.descripcion}
                        </span>
                      )}
                      {candidato.ponderaciones?.length > 0 && (
                        <span className="mt-1 block text-gray-600">
                          Ponderaciones:{" "}
                          {candidato.ponderaciones
                            .map(
                              (item) =>
                                `${item.ordenVisualizacion}. ${item.descripcion}: ${item.ponderacion}`
                            )
                            .join(" · ")}
                        </span>
                      )}
                    </span>
                  </label>
                ))}
                <button
                  type="button"
                  onClick={restaurarCandidato}
                  disabled={
                    !codigoCandidatoSeleccionado || restaurando || enviando
                  }
                  className="rounded bg-black px-5 py-2 text-sm font-medium text-white hover:bg-gray-800 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {restaurando ? "Restaurando..." : "Restaurar seleccionada"}
                </button>
              </div>
            ) : (
              <p className="mt-2 text-sm">
                Modifique el nombre, la pregunta o el factor antes de volver a
                intentar el alta.
              </p>
            )}
          </section>
        )}

        <form
          onSubmit={enviarAlta}
          noValidate
          className="rounded-lg border border-gray-400 p-4 sm:p-6"
        >
          <div className="grid grid-cols-1 gap-x-8 gap-y-5 md:grid-cols-3">
            <div className="flex flex-col">
              <label
                htmlFor="codigoCompetencia"
                className="mb-1 text-sm font-medium"
              >
                Competencia <span aria-hidden="true">(*)</span>
              </label>
              <select
                id="codigoCompetencia"
                value={formulario.codigoCompetencia}
                onChange={manejarCambioCompetencia}
                disabled={cargandoCompetencias || enviando || restaurando}
                aria-invalid={Boolean(erroresCampo.codigoCompetencia)}
                aria-describedby={
                  erroresCampo.codigoCompetencia
                    ? "error-codigoCompetencia"
                    : undefined
                }
                className="rounded border border-gray-400 bg-white px-3 py-2 text-sm focus:border-black focus:outline-none disabled:bg-gray-100"
              >
                <option value="">
                  {cargandoCompetencias
                    ? "Cargando competencias..."
                    : "Seleccione una competencia"}
                </option>
                {competencias.map((competencia) => (
                  <option key={competencia.codigo} value={competencia.codigo}>
                    {competencia.nombre}
                  </option>
                ))}
              </select>
              {erroresCampo.codigoCompetencia && (
                <p id="error-codigoCompetencia" className="mt-1 text-xs text-rose-700">
                  {erroresCampo.codigoCompetencia}
                </p>
              )}
            </div>

            <div className="flex flex-col">
              <label htmlFor="codigoFactor" className="mb-1 text-sm font-medium">
                Factor <span aria-hidden="true">(*)</span>
              </label>
              <select
                id="codigoFactor"
                value={formulario.codigoFactor}
                onChange={(evento) => {
                  setFormulario((actual) => ({
                    ...actual,
                    codigoFactor: evento.target.value,
                  }));
                  setErroresCampo((actuales) => ({
                    ...actuales,
                    codigoFactor: undefined,
                  }));
                  limpiarConflictoSiCambiaClave("codigoFactor");
                }}
                disabled={
                  !formulario.codigoCompetencia ||
                  cargandoFactores ||
                  enviando ||
                  restaurando
                }
                aria-invalid={Boolean(erroresCampo.codigoFactor)}
                aria-describedby={
                  erroresCampo.codigoFactor ? "error-codigoFactor" : undefined
                }
                className="rounded border border-gray-400 bg-white px-3 py-2 text-sm focus:border-black focus:outline-none disabled:bg-gray-100"
              >
                <option value="">
                  {cargandoFactores
                    ? "Cargando factores..."
                    : formulario.codigoCompetencia
                      ? "Seleccione un factor"
                      : "Seleccione primero una competencia"}
                </option>
                {factores.map((factor) => (
                  <option key={factor.codigo} value={factor.codigo}>
                    {factor.nombre}
                  </option>
                ))}
              </select>
              {erroresCampo.codigoFactor && (
                <p id="error-codigoFactor" className="mt-1 text-xs text-rose-700">
                  {erroresCampo.codigoFactor}
                </p>
              )}
            </div>

            <div className="flex flex-col">
              <label htmlFor="nombre" className="mb-1 text-sm font-medium">
                Nombre de Pregunta <span aria-hidden="true">(*)</span>
              </label>
              <input
                id="nombre"
                type="text"
                maxLength={100}
                value={formulario.nombre}
                onChange={(evento) =>
                  actualizarTexto("nombre", evento.target.value)
                }
                disabled={enviando || restaurando}
                aria-invalid={Boolean(erroresCampo.nombre)}
                aria-describedby={
                  erroresCampo.nombre ? "error-nombre" : undefined
                }
                placeholder="Ej.: Lado de los círculos"
                className="rounded border border-gray-400 px-3 py-2 text-sm focus:border-black focus:outline-none disabled:bg-gray-100"
              />
              {erroresCampo.nombre && (
                <p id="error-nombre" className="mt-1 text-xs text-rose-700">
                  {erroresCampo.nombre}
                </p>
              )}
            </div>

            <div className="flex flex-col">
              <label htmlFor="pregunta" className="mb-1 text-sm font-medium">
                Pregunta <span aria-hidden="true">(*)</span>
              </label>
              <input
                id="pregunta"
                type="text"
                maxLength={150}
                value={formulario.pregunta}
                onChange={(evento) =>
                  actualizarTexto("pregunta", evento.target.value)
                }
                disabled={enviando || restaurando}
                aria-invalid={Boolean(erroresCampo.pregunta)}
                aria-describedby={
                  erroresCampo.pregunta ? "error-pregunta" : undefined
                }
                placeholder="Ej.: ¿Cuántos lados tiene un círculo?"
                className="rounded border border-gray-400 px-3 py-2 text-sm focus:border-black focus:outline-none disabled:bg-gray-100"
              />
              {erroresCampo.pregunta && (
                <p id="error-pregunta" className="mt-1 text-xs text-rose-700">
                  {erroresCampo.pregunta}
                </p>
              )}
            </div>

            <div className="flex flex-col">
              <label htmlFor="descripcion" className="mb-1 text-sm font-medium">
                Descripción
              </label>
              <textarea
                id="descripcion"
                maxLength={500}
                rows={5}
                value={formulario.descripcion}
                onChange={(evento) =>
                  actualizarTexto("descripcion", evento.target.value)
                }
                disabled={enviando || restaurando}
                aria-invalid={Boolean(erroresCampo.descripcion)}
                aria-describedby={
                  erroresCampo.descripcion ? "error-descripcion" : undefined
                }
                placeholder="Opcional"
                className="resize-y rounded border border-gray-400 px-3 py-2 text-sm focus:border-black focus:outline-none disabled:bg-gray-100"
              />
              {erroresCampo.descripcion && (
                <p id="error-descripcion" className="mt-1 text-xs text-rose-700">
                  {erroresCampo.descripcion}
                </p>
              )}
            </div>

            <div className="flex flex-col">
              <label
                htmlFor="idOpcionRespuesta"
                className="mb-1 text-sm font-medium"
              >
                Opción de Respuesta <span aria-hidden="true">(*)</span>
              </label>
              <select
                id="idOpcionRespuesta"
                value={formulario.idOpcionRespuesta}
                onChange={manejarCambioOpcion}
                disabled={cargandoOpciones || enviando || restaurando}
                aria-invalid={Boolean(erroresCampo.idOpcionRespuesta)}
                aria-describedby={
                  erroresCampo.idOpcionRespuesta
                    ? "error-idOpcionRespuesta"
                    : undefined
                }
                className="rounded border border-gray-400 bg-white px-3 py-2 text-sm focus:border-black focus:outline-none disabled:bg-gray-100"
              >
                <option value="">
                  {cargandoOpciones
                    ? "Cargando opciones..."
                    : "Seleccione una opción"}
                </option>
                {opcionesRespuesta.map((opcion) => (
                  <option
                    key={opcion.idOpcionRespuesta}
                    value={opcion.idOpcionRespuesta}
                  >
                    {opcion.nombre}
                  </option>
                ))}
              </select>
              {erroresCampo.idOpcionRespuesta && (
                <p
                  id="error-idOpcionRespuesta"
                  className="mt-1 text-xs text-rose-700"
                >
                  {erroresCampo.idOpcionRespuesta}
                </p>
              )}
            </div>
          </div>

          <section className="mt-6" aria-labelledby="titulo-ponderaciones">
            <div className="mb-2 flex flex-wrap items-baseline justify-between gap-2">
              <h2 id="titulo-ponderaciones" className="text-sm font-semibold">
                Ponderaciones por ítem
              </h2>
              {formulario.idOpcionRespuesta && (
                <p
                  aria-live="polite"
                  className={`text-sm font-medium ${
                    sumaCentavos === SUMA_REQUERIDA_CENTAVOS
                      ? "text-green-700"
                      : "text-gray-700"
                  }`}
                >
                  Suma: {formatearCentavos(sumaCentavos)} / 10.00
                </p>
              )}
            </div>

            {cargandoItems && (
              <p className="mb-2 text-sm text-gray-600">Cargando ítems...</p>
            )}

            <div className="overflow-x-auto rounded border border-gray-400">
              <table className="w-full min-w-[34rem] border-collapse text-left text-sm">
                <thead className="bg-gray-100">
                  <tr>
                    <th scope="col" className="border-b border-r px-3 py-2">
                      Orden
                    </th>
                    <th scope="col" className="border-b border-r px-3 py-2">
                      Descripción
                    </th>
                    <th scope="col" className="border-b px-3 py-2">
                      Ponderación
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {!formulario.idOpcionRespuesta ? (
                    <tr>
                      <td colSpan={3} className="px-3 py-4 text-center text-gray-600">
                        Seleccione una opción de respuesta para ver sus ítems.
                      </td>
                    </tr>
                  ) : !cargandoItems && items.length === 0 ? (
                    <tr>
                      <td colSpan={3} className="px-3 py-4 text-center text-gray-600">
                        No hay ítems disponibles para esta opción.
                      </td>
                    </tr>
                  ) : (
                    items.map((item) => {
                      const errorPonderacion =
                        erroresCampo[`ponderaciones.${item.idItemOpcion}`];
                      return (
                        <tr key={item.idItemOpcion}>
                          <td className="border-b border-r px-3 py-2">
                            {item.ordenVisualizacion}
                          </td>
                          <td className="border-b border-r px-3 py-2">
                            {item.descripcion}
                          </td>
                          <td className="border-b px-3 py-2">
                            <input
                              type="text"
                              inputMode="decimal"
                              value={
                                ponderaciones[item.idItemOpcion] ?? ""
                              }
                              onChange={(evento) =>
                                actualizarPonderacion(
                                  item.idItemOpcion,
                                  evento.target.value
                                )
                              }
                              disabled={cargandoItems || enviando || restaurando}
                              aria-label={`Ponderación para ${item.descripcion}`}
                              aria-invalid={Boolean(errorPonderacion)}
                              aria-describedby={
                                errorPonderacion
                                  ? `error-ponderacion-${item.idItemOpcion}`
                                  : undefined
                              }
                              className="w-32 rounded border border-gray-400 px-3 py-1.5 focus:border-black focus:outline-none disabled:bg-gray-100"
                            />
                            {errorPonderacion && (
                              <p
                                id={`error-ponderacion-${item.idItemOpcion}`}
                                className="mt-1 text-xs text-rose-700"
                              >
                                {errorPonderacion}
                              </p>
                            )}
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>
            {erroresCampo.ponderaciones && (
              <p className="mt-2 text-sm text-rose-700" role="alert">
                {erroresCampo.ponderaciones}
              </p>
            )}
          </section>

          <div className="mt-7 flex flex-col-reverse justify-end gap-3 sm:flex-row">
            <button
              type="button"
              onClick={() => router.push("/api/preguntas")}
              disabled={enviando || restaurando}
              className="rounded bg-black px-7 py-2 text-sm font-medium text-white hover:bg-gray-800 disabled:cursor-not-allowed disabled:opacity-50"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={bloquearAlta}
              className="rounded bg-black px-7 py-2 text-sm font-medium text-white hover:bg-gray-800 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {enviando ? "Guardando..." : "Aceptar"}
            </button>
          </div>
        </form>
      </div>

      {altaExitosa && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="alta-exitosa-titulo"
          className="fixed inset-0 z-10 flex items-center justify-center bg-black/40 p-4"
        >
          <section className="w-full max-w-md rounded-lg bg-white p-6 shadow-xl">
            <h2 id="alta-exitosa-titulo" className="text-lg font-semibold">
              Pregunta creada exitosamente
            </h2>
            <p className="mt-2 text-sm">¿Desea cargar otra pregunta?</p>
            <div className="mt-6 flex justify-end gap-3">
              <button
                type="button"
                onClick={() => router.push("/api/preguntas")}
                className="rounded bg-black px-5 py-2 text-sm font-medium text-white hover:bg-gray-800"
              >
                No
              </button>
              <button
                type="button"
                onClick={reiniciarFormulario}
                className="rounded bg-black px-5 py-2 text-sm font-medium text-white hover:bg-gray-800"
              >
                Sí
              </button>
            </div>
          </section>
        </div>
      )}

      {restauracionExitosa && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="restauracion-exitosa-titulo"
          className="fixed inset-0 z-10 flex items-center justify-center bg-black/40 p-4"
        >
          <section className="w-full max-w-md rounded-lg bg-white p-6 shadow-xl">
            <h2 id="restauracion-exitosa-titulo" className="text-lg font-semibold">
              Pregunta restaurada exitosamente
            </h2>
            <p className="mt-2 text-sm">
              Se restauró “{restauracionExitosa.nombre}”.
            </p>
            <div className="mt-6 flex justify-end">
              <button
                type="button"
                onClick={() => router.push("/api/preguntas")}
                className="rounded bg-black px-5 py-2 text-sm font-medium text-white hover:bg-gray-800"
              >
                Volver al gestor
              </button>
            </div>
          </section>
        </div>
      )}
    </main>
  );
}
