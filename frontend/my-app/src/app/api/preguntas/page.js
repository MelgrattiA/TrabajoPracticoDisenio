"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";

const API_URL = "http://localhost:8080/api/preguntas";

export default function GestorPreguntasPage() {
    const router = useRouter();

    const [competenciasDisponibles, setCompetenciasDisponibles] = useState([]);
    const [competencia, setCompetencia] = useState("");
    const [factor, setFactor] = useState("");
    const [nombrePregunta, setNombrePregunta] = useState("");
    
    const [preguntas, setPreguntas] = useState([]);
    const [busquedaRealizada, setBusquedaRealizada] = useState(false);
    const [cargando, setCargando] = useState(false);
    const [mensajeError, setMensajeError] = useState("");

    useEffect(() => {
        const cargarCompetencias = async () => {
            try {
                const response = await fetch(`${API_URL}/competencias`);
                if (response.ok) {
                    const data = await response.json();
                    setCompetenciasDisponibles(data);
                }
            } catch {
                setCompetenciasDisponibles([]);
            }
        };

        cargarCompetencias();
    }, []);

    const handleBuscar = async (e) => {
        if (e) {
            e.preventDefault();
        }
        setMensajeError("");

        if (!competencia.trim() && !factor.trim() && !nombrePregunta.trim()) {
            setMensajeError("Ingrese al menos un campo para poder realizar la busqueda.");
            setBusquedaRealizada(false);
            setPreguntas([]);
        } else {
            try {
                setCargando(true);

                const params = new URLSearchParams();
                if (competencia.trim()) {
                    params.append("competencia", competencia.trim());
                }
                if (factor.trim()) {
                    params.append("factor", factor.trim());
                }
                if (nombrePregunta.trim()) {
                    params.append("pregunta", nombrePregunta.trim());
                }

                const response = await fetch(`${API_URL}?${params.toString()}`);

                if (!response.ok) {
                    if (response.status >= 500) {
                        setMensajeError("Error interno del servidor al buscar las preguntas.");
                    } else {
                        const errorTexto = await response.text();
                        setMensajeError(errorTexto || "Ocurrió un error al buscar las preguntas.");
                    }
                    setBusquedaRealizada(false);
                } else {
                    const data = await response.json();
                    setPreguntas(data);
                    setBusquedaRealizada(true);
                }
            } catch {
                setMensajeError("No se pudo conectar con el servidor backend.");
            } finally {
                setCargando(false);
            }
        }
    };

    const handleNuevo = () => {
        router.push("/preguntas/nuevo");
    };

    const handleSeleccionar = (item) => {
        sessionStorage.setItem("preguntaSeleccionada", JSON.stringify(item));
        router.push("/");
    };

    const handleModificar = (item) => {
        const params = new URLSearchParams({
            competencia: item.competencia,
            factor: item.factor,
            pregunta: item.pregunta,
        });
        router.push(`/preguntas/modificar?${params.toString()}`);
    };

    const handleEliminar = (item) => {
        const params = new URLSearchParams({
            competencia: item.competencia,
            factor: item.factor,
            pregunta: item.pregunta,
        });
        router.push(`/preguntas/eliminar?${params.toString()}`);
    };

    const handleSalir = () => {
        router.push("/");
    };

    return (
        <div className="min-h-screen bg-white text-black p-8 flex flex-col justify-between">
            <div className="max-w-5xl w-full mx-auto">
                {/* ENCABEZADO */}
                <header className="flex items-center justify-between border-b border-gray-400 pb-3 mb-8">
                    <h1 className="text-4xl font-bold tracking-tight">
                        Gestor de Preguntas
                    </h1>

                    <div className="flex items-center gap-3 text-right">
                        <div className="text-xs leading-tight text-gray-700">
                            <p className="font-semibold">&lt;Nombre usuario&gt;</p>
                            <p>[rol]</p>
                        </div>
                        {/* Icono usuario */}
                        <div className="w-10 h-10 border border-gray-500 flex items-center justify-center text-gray-500">
                            ✕
                        </div>
                    </div>
                </header>

                {/* FORMULARIO DE BÚSQUEDA */}
                <form onSubmit={handleBuscar}>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-8 mb-8">
                        <div className="flex flex-col">
                            <label className="text-xs font-medium mb-1">Competencia</label>
                            <select
                                value={competencia}
                                onChange={(e) => setCompetencia(e.target.value)}
                                className="border border-gray-400 rounded px-3 py-1.5 text-sm bg-white focus:outline-none focus:border-black"
                            >
                                <option value="">Select option</option>
                                {competenciasDisponibles.map((comp, idx) => {
                                    const nombreComp = typeof comp === "string" ? comp : comp.nombre;
                                    const keyComp = typeof comp === "string" ? comp : (comp.codigo ?? idx);
                                    return (
                                        <option key={keyComp} value={nombreComp}>
                                            {nombreComp}
                                        </option>
                                    );
                                })}
                            </select>
                        </div>

                        <div className="flex flex-col">
                            <label className="text-xs font-medium mb-1">Factor</label>
                            <input
                                type="text"
                                placeholder="Type here..."
                                value={factor}
                                onChange={(e) => setFactor(e.target.value)}
                                className="border border-gray-400 rounded px-3 py-1.5 text-sm focus:outline-none focus:border-black"
                            />
                        </div>

                        <div className="flex flex-col">
                            <label className="text-xs font-medium mb-1">Nombre Pregunta</label>
                            <input
                                type="text"
                                placeholder="Type here..."
                                value={nombrePregunta}
                                onChange={(e) => setNombrePregunta(e.target.value)}
                                className="border border-gray-400 rounded px-3 py-1.5 text-sm focus:outline-none focus:border-black"
                            />
                        </div>
                    </div>

                    {/* BOTONES BUSCAR Y NUEVO */}
                    <div className="flex items-center justify-between mb-6">
                        <button
                            type="submit"
                            disabled={cargando}
                            className="bg-black text-white text-sm font-medium px-8 py-2 rounded hover:bg-gray-800 cursor-pointer disabled:opacity-50"
                        >
                            {cargando ? "Buscando..." : "Buscar"}
                        </button>

                        <button
                            type="button"
                            onClick={handleNuevo}
                            className="bg-black text-white text-sm font-medium px-8 py-2 rounded hover:bg-gray-800 cursor-pointer"
                        >
                            Nuevo
                        </button>
                    </div>
                </form>

                {/* CARTEL ROJO DE VALIDACIÓN */}
                {mensajeError && (
                    <div className="flex justify-center my-4">
                        <div className="bg-red-500 text-black font-medium text-sm px-6 py-4 rounded-lg shadow-lg max-w-xs text-center">
                            {mensajeError}
                        </div>
                    </div>
                )}

                {/* GRILLA DE PREGUNTAS COINCIDENTES */}
                {busquedaRealizada && (
                    <div className="mt-6 border border-gray-500">
                        <div className="px-3 py-1.5 border-b border-gray-500 text-xs font-medium bg-gray-50">
                            Preguntas Coincidentes:
                        </div>

                        {preguntas.length === 0 ? (
                            <div className="px-3 py-4 text-sm text-gray-600">
                                No se encontraron preguntas que coincidan con los criterios ingresados.
                            </div>
                        ) : (
                            <ul>
                                {preguntas.map((item, index) => (
                                    <li
                                        key={index}
                                        className="flex flex-col sm:flex-row sm:items-center justify-between px-3 py-2 border-b last:border-b-0 border-gray-400 text-sm gap-2"
                                    >
                                        <span>
                                            {item.pregunta} - {item.factor} - {item.competencia}
                                        </span>

                                        <div className="flex items-center gap-2 shrink-0">
                                            <button
                                                type="button"
                                                onClick={() => handleSeleccionar(item)}
                                                className="bg-black text-white text-xs px-3 py-1 rounded hover:bg-gray-800 cursor-pointer"
                                            >
                                                Seleccionar
                                            </button>
                                            <button
                                                type="button"
                                                onClick={() => handleModificar(item)}
                                                className="bg-black text-white text-xs px-3 py-1 rounded hover:bg-gray-800 cursor-pointer"
                                            >
                                                Modificar
                                            </button>
                                            <button
                                                type="button"
                                                onClick={() => handleEliminar(item)}
                                                className="bg-black text-white text-xs px-3 py-1 rounded hover:bg-gray-800 cursor-pointer"
                                            >
                                                Eliminar
                                            </button>
                                        </div>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
                )}
            </div>

            {/* BOTÓN SALIR ABAJO A LA IZQUIERDA */}
            <footer className="max-w-5xl w-full mx-auto pt-12">
                <button
                    type="button"
                    onClick={handleSalir}
                    className="bg-black text-white text-sm font-medium px-9 py-2 rounded hover:bg-gray-800 cursor-pointer"
                >
                    Salir
                </button>
            </footer>
        </div>
    );
}