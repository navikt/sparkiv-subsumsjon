export interface Subsumsjon {
    type: string
    lovverk: string
    utfall: string
    versjon: string
    paragraf: string
    ledd: string | null
    punktum: string | null
    bokstav: string | null
    input: Record<string, unknown>
    output: Record<string, unknown>
}

export interface Melding {
    id: string
    eventName: string
    tidsstempel: string
    fodselsnummer: string
    organisasjonsnummer?: string
    vedtaksperiodeId: string
    behandlingId?: string
    subsumsjon?: Subsumsjon
    [key: string]: unknown
}

export interface Gruppe {
    vedtaksperiodeId: string
    behandlingId: string
    meldinger: Melding[]
}

export function grupperMeldinger(meldinger: Melding[]): Gruppe[] {
    const grupper = new Map<string, Gruppe>()
    for (const melding of meldinger) {
        const behandlingId = melding.behandlingId ?? 'ukjent'
        const key = `${melding.vedtaksperiodeId}|${behandlingId}`
        let gruppe = grupper.get(key)
        if (!gruppe) {
            gruppe = { vedtaksperiodeId: melding.vedtaksperiodeId, behandlingId, meldinger: [] }
            grupper.set(key, gruppe)
        }
        gruppe.meldinger.push(melding)
    }
    for (const gruppe of grupper.values()) {
        gruppe.meldinger.sort((a, b) => {
            const paragrafA = a.subsumsjon?.paragraf ?? (a.paragraf as string | undefined) ?? ''
            const paragrafB = b.subsumsjon?.paragraf ?? (b.paragraf as string | undefined) ?? ''
            return paragrafA.localeCompare(paragrafB, 'nb-NO', { numeric: true }) || a.tidsstempel.localeCompare(b.tidsstempel)
        })
    }
    return Array.from(grupper.values()).sort((a, b) => tidligsteTidsstempel(a).localeCompare(tidligsteTidsstempel(b)))
}

function tidligsteTidsstempel(gruppe: Gruppe): string {
    return gruppe.meldinger.reduce(
        (tidligst, melding) => (melding.tidsstempel < tidligst ? melding.tidsstempel : tidligst),
        gruppe.meldinger[0].tidsstempel
    )
}
