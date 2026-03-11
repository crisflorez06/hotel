export function getCurrentDateInput(): string {
  return new Date().toISOString().slice(0, 10);
}

export function formatDateTimeNoSeconds(fecha: string): string {
  const fechaObj = new Date(fecha);
  if (Number.isNaN(fechaObj.getTime())) {
    return fecha;
  }

  return fechaObj.toLocaleString('es-CO', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function formatDateOnly(fecha: string): string {
  const fechaObj = new Date(fecha);
  if (Number.isNaN(fechaObj.getTime())) {
    return fecha;
  }

  return fechaObj.toLocaleDateString('es-CO', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
}
