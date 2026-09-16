import type { Melding } from './types'

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export class ApiError extends Error {
    status: number

    constructor(message: string, status: number) {
        super(message)
        this.status = status
    }
}

async function hent(url: string): Promise<Melding[]> {
    const response = await fetch(url)

    if (response.status === 404) {
        return []
    }
    if (!response.ok) {
        throw new ApiError(await response.text(), response.status)
    }
    return (await response.json()) as Melding[]
}

export function hentMeldinger(vedtaksperiodeId: string): Promise<Melding[]> {
    return hent(`${API_URL}/vedtaksperiode/${encodeURIComponent(vedtaksperiodeId)}`)
}

export function hentMeldingerForFodselsnummer(fodselsnummer: string): Promise<Melding[]> {
    return hent(`${API_URL}/fodselsnummer/${encodeURIComponent(fodselsnummer)}`)
}
