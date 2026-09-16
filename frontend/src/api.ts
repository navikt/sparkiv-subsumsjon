import type { Melding } from './types'

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
    return hent(`/vedtaksperiode/${encodeURIComponent(vedtaksperiodeId)}`)
}

export function hentMeldingerForFodselsnummer(fodselsnummer: string): Promise<Melding[]> {
    return hent(`/fodselsnummer/${encodeURIComponent(fodselsnummer)}`)
}
