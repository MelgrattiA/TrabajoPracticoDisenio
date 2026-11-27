"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";

const API_BASE = "http://localhost:8080/api/cuestionarios";

const ESTADOS_PANTALLA = {
  NORMAL: "normal",
  MAX_ACCESOS: "max-accesos",
  TIEMPO_COMPLETAR: "tiempo-completar",
  TIEMPO_INICIO: "tiempo-inicio",
};

const MENSAJES_ESTADO = {
  [ESTADOS_PANTALLA.MAX_ACCESOS]:
    "Superaste la cantidad maxima de accesos permitidos al cuestionario.",
  [ESTADOS_PANTALLA.TIEMPO_COMPLETAR]:
    "Se supero el tiempo limite para terminar de completar el cuestionario.",
  [ESTADOS_PANTALLA.TIEMPO_INICIO]:
    "Se supero el tiempo limite para iniciar el cuestionario.",
  completo: "Este cuestionario ya fue completado.",
};

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

function TablaParametros({ configuracion }) {
  const parametros = [
    ["Cantidad de preguntas por bloque:", configuracion?.cantidadPreguntasPorBloque],
    ["Cantidad de preguntas por factor:", configuracion?.cantidadPreguntasPorFactor],
    ["Máximo de accesos permitidos al cuestionario:", configuracion?.maximoAccesos],
    [
      "Tiempo máximo para completar el cuestionario:",
      configuracion?.tiempoMaximoCompletarDias,
      "día(s)",
    ],
    [
      "Tiempo máximo permitido para empezar a completar el cuestionario:",
      configuracion?.tiempoMaximoActivoDias,
      "día(s)",
    ],
  ];
  return (
    <div className="w-full max-w-4xl overflow-hidden border border-gray-500">
      <table className="w-full border-collapse text-sm text-left">
        <thead className="bg-gray-100 sticky top-0">
          <tr>
            <th className="w-3/4 border-b border-r border-gray-400 px-3 py-2 font-semibold">
              Instrucción:
            </th>
            <th className="border-b border-gray-400 px-3 py-2 font-semibold">
              Valor:
            </th>
          </tr>
        </thead>
        <tbody>
          {parametros.map(([nombre, valor, unidad]) => (
            <tr key={nombre} className="hover:bg-gray-50">
              <td className="border-b border-r border-gray-300 px-3 py-2">
                {nombre}
              </td>
              <td className="border-b border-gray-300 px-3 py-2">
                {valor == null ? "—" : `${valor}${unidad ? ` ${unidad}` : ""}`}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default function InstruccionesCuestionarioPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [estadoPantalla, setEstadoPantalla] = useState(ESTADOS_PANTALLA.NORMAL);
  const [mensajeEstado, setMensajeEstado] = useState("");
  const [instrucciones, setInstrucciones] = useState("");
  const [configuracion, setConfiguracion] = useState(null);
  const [cargandoEstado, setCargandoEstado] = useState(true);
  const [iniciando, setIniciando] = useState(false);
  const [claveIngresada, setClaveIngresada] = useState("");

  const clave = useMemo(() => {
    const valor = (searchParams.get("clave") || "").trim();
    return valor;
  }, [searchParams]);

  const consultarCuestionario = (event) => {
    event.preventDefault();
    const claveAConsultar = (clave || claveIngresada).trim();
    if (claveAConsultar) {
      router.push(
        `/api/cuestionario/instrucciones?clave=${encodeURIComponent(claveAConsultar)}`
      );
    }
  };

  const handleAceptar = async () => {
    if (!clave) {
      setMensajeEstado("Falta la clave del cuestionario.");
      return;
    }

    try {
      setIniciando(true);
      const response = await fetch(
        `${API_BASE}/${encodeURIComponent(clave)}/iniciar`,
        { method: "POST" }
      );
      const payload = await response.json();
      if (!response.ok) {
        throw new Error(payload?.detail || "No se pudo iniciar el cuestionario.");
      }

      router.push(
        `/api/cuestionario/responder?clave=${encodeURIComponent(clave)}`
      );
    } catch (error) {
      setMensajeEstado(error.message || "No se pudo iniciar el cuestionario.");
    } finally {
      setIniciando(false);
    }
  };

  useEffect(() => {
    let cancelado = false;

    const cargarEstado = async () => {
      // Sin una clave no se puede consultar el cuestionario en el backend.
      if (!clave) {
        setEstadoPantalla("sin-clave");
        setMensajeEstado("Ingresá la clave que recibiste para tu cuestionario.");
        setInstrucciones("");
        setConfiguracion(null);
        setCargandoEstado(false);
        return;
      }

      try {
        setCargandoEstado(true);
        const response = await fetch(
          `${API_BASE}/${encodeURIComponent(clave)}/instrucciones`
        );

        let payload = null;
        try {
          payload = await response.json();
        } catch {
          payload = null;
        }

        if (!response.ok) {
          const detalle = payload?.detail || "No se pudo validar el estado del cuestionario.";
          throw new Error(detalle);
        }

        const estadoBackend = (payload?.estadoPantalla || "").trim();
        if (!estadoBackend) {
          throw new Error("El backend no devolvio un estado de pantalla valido.");
        }

        if (!cancelado) {
          setEstadoPantalla(estadoBackend);
          setMensajeEstado((payload?.mensaje || MENSAJES_ESTADO[estadoBackend] || "").trim());
          setInstrucciones(payload?.instrucciones || "");
          setConfiguracion(payload);
        }
      } catch (error) {
        if (!cancelado) {
          setEstadoPantalla("error");
          setMensajeEstado(error.message || "Error inesperado al consultar el backend.");
        }
      } finally {
        if (!cancelado) {
          setCargandoEstado(false);
        }
      }
    };

    cargarEstado();

    return () => {
      cancelado = true;
    };
  }, [clave]);

  return (
    <div className="min-h-screen bg-white text-black flex flex-col p-6 md:p-10">
      <header className="flex items-center justify-between border-b border-gray-400 pb-3">
        <h1 className="text-3xl md:text-5xl font-semibold tracking-tight">
          Cuestionario - Instrucciones
        </h1>

        <div className="flex items-center gap-3 text-right">
          <div className="text-xs text-gray-700 leading-tight">
            <p>&lt;Nombre usuario&gt;</p>
            <p>[rol]</p>
          </div>
          <AvatarPlaceholder />
        </div>
      </header>

      <main className="mx-auto flex w-full max-w-6xl flex-1 flex-col items-center px-2 pt-12 md:px-8 md:pt-16">
        <h2 className="mb-10 text-center text-4xl font-semibold md:mb-12 md:text-5xl">
          Empezar Cuestionario:
        </h2>

        <p className="mb-8 max-w-4xl text-center text-lg leading-relaxed md:text-xl">
          {instrucciones ||
            "No se recibieron instrucciones del cuestionario."}
        </p>

        <TablaParametros configuracion={configuracion} />

        {cargandoEstado ? (
          <div role="status" className="mt-20 text-base text-gray-700">
            Cargando estado del cuestionario...
          </div>
        ) : estadoPantalla === ESTADOS_PANTALLA.NORMAL ? (
          <div className="mt-32 flex items-center justify-center gap-10 md:mt-40 md:gap-24">
            <button
              type="button"
              onClick={handleAceptar}
              disabled={iniciando || !clave}
              className="cursor-pointer rounded bg-black px-5 py-2 text-2xl font-medium leading-none text-white transition hover:bg-gray-800 disabled:cursor-not-allowed disabled:opacity-50 md:text-3xl"
            >
              {iniciando ? "Iniciando..." : "Aceptar"}
            </button>
            <button
              type="button"
              onClick={() => router.push("/")}
              className="cursor-pointer rounded bg-black px-5 py-2 text-2xl font-medium leading-none text-white transition hover:bg-gray-800 md:text-3xl"
            >
              Cancelar
            </button>
          </div>
        ) : (
          <div
            role="alert"
            className="mt-20 max-w-2xl rounded-2xl bg-rose-500 px-8 py-6 text-center text-xl leading-tight text-black shadow-lg"
          >
            {mensajeEstado}
          </div>
        )}

        <form
          onSubmit={consultarCuestionario}
          className="mt-auto flex w-full justify-end pt-12"
        >
          <div className="w-full max-w-[260px]">
            <label className="block text-sm font-medium">
              Clave del cuestionario
              <input
                type="text"
                value={clave || claveIngresada}
                onChange={(event) => setClaveIngresada(event.target.value)}
                readOnly={Boolean(clave)}
                className="mt-1 w-full border-b border-gray-400 px-3 py-2 font-normal outline-none focus:border-black"
                required
              />
            </label>
            {!clave && (
              <button
                type="submit"
                className="mt-2 rounded bg-black px-4 py-1.5 text-sm text-white hover:bg-gray-800"
              >
                Consultar
              </button>
            )}
          </div>
        </form>
      </main>
    </div>
  );
}
