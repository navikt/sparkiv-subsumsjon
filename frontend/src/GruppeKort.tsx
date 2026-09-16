import { Accordion, BodyShort, Box, Heading, HGrid, Label, Table, VStack } from '@navikt/ds-react'
import type { Melding, Gruppe } from './types'

function visFelt(verdi: unknown) {
    return verdi === null || verdi === undefined || verdi === '' ? '–' : String(verdi)
}

// Tekstrepresentasjon av en enkel verdi (primitiv, eller liste av primitiver).
function verdiTekst(verdi: unknown): string {
    if (verdi === null || verdi === undefined || verdi === '') return '–'
    if (Array.isArray(verdi)) {
        if (verdi.length === 0) return '–'
        return verdi.map((v) => String(v)).join(', ')
    }
    return String(verdi)
}

function erEnkelVerdi(verdi: unknown) {
    if (verdi === null || verdi === undefined || typeof verdi !== 'object') return true
    return Array.isArray(verdi) && verdi.every((v) => typeof v !== 'object' || v === null)
}

// Viser kun klokkeslett (timer:minutter:sekunder) istedenfor hele ISO-tidsstempelet.
function visKlokkeslett(tidsstempel: string) {
    const dato = new Date(tidsstempel)
    if (Number.isNaN(dato.getTime())) return tidsstempel
    return dato.toLocaleTimeString('nb-NO', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

// Viser dato (dd.mm.åååå) istedenfor hele ISO-tidsstempelet.
function visDato(tidsstempel: string) {
    const dato = new Date(tidsstempel)
    if (Number.isNaN(dato.getTime())) return tidsstempel
    return dato.toLocaleDateString('nb-NO')
}

const etiketter: Record<string, string> = {
    id: 'Id',
    tidsstempel: 'Tidsstempel',
    fodselsnummer: 'Fødselsnummer',
    organisasjonsnummer: 'Organisasjonsnummer',
    vedtaksperiodeId: 'VedtaksperiodeId',
    behandlingId: 'BehandlingId',
    kilde: 'Kilde',
    utfall: 'Utfall',
    input: 'Input',
    output: 'Output',
    sporing: 'Sporing',
    lovverksversjon: 'Lovverksversjon',
    type: 'Type'
}

function etikett(nokkel: string) {
    return etiketter[nokkel] ?? nokkel
}

// Innrykk + venstre kant-linje for nøstet innhold (input/output/sporing etc.), slik at kant og
// innrykk henger sammen på samme boks — border og padding ligger her på samme element.
function Nestet({ children }: { children: React.ReactNode }) {
    return (
        <Box paddingInline="space-16 space-0">
            <VStack gap="space-2">{children}</VStack>
        </Box>
    )
}

// Rendrer en verdi menneskelesbart: primitiver rett fram, lister som kommaseparert,
// og objekter som en nestet liste av felt/verdi-par (i stedet for rå JSON).
function Verdi({ verdi }: { verdi: unknown }) {
    if (erEnkelVerdi(verdi)) {
        return <BodyShort>{verdiTekst(verdi)}</BodyShort>
    }
    if (Array.isArray(verdi)) {
        return (
            <Nestet>
                {verdi.map((v, i) => (
                    <Verdi verdi={v} key={i} />
                ))}
            </Nestet>
        )
    }
    if (typeof verdi === 'object') {
        const oppforinger = Object.entries(verdi as Record<string, unknown>)
        if (oppforinger.length === 0) return <BodyShort>–</BodyShort>
        return (
            <Nestet>
                {oppforinger.map(([nokkel, v]) => (
                    <FeltInnhold nokkel={nokkel} verdi={v} key={nokkel} />
                ))}
            </Nestet>
        )
    }
    return <BodyShort>{String(verdi)}</BodyShort>
}

// Viser feltnavn: verdi på én linje for enkle verdier, ellers feltnavn over et nøstet innhold.
function FeltInnhold({ nokkel, verdi, toppniva = false }: { nokkel: string; verdi: unknown; toppniva?: boolean }) {
    if (erEnkelVerdi(verdi)) {
        return (
            <BodyShort>
                <strong>{etikett(nokkel)}:</strong> {verdiTekst(verdi)}
            </BodyShort>
        )
    }
    return (
        <div>
            {toppniva ? (
                <Heading level="4" size="xsmall" spacing>
                    {etikett(nokkel)}
                </Heading>
            ) : (
                <Label spacing>{etikett(nokkel)}</Label>
            )}
            <Verdi verdi={verdi} />
        </div>
    )
}

function Felt({ nokkel, verdi }: { nokkel: string; verdi: unknown }) {
    return (
        <Box padding="space-4" borderRadius="8" background="neutral-soft">
            <FeltInnhold nokkel={nokkel} verdi={verdi} toppniva />
        </Box>
    )
}

// Rekkefølge på feltene som vises når man ekspanderer en subsumsjon.
const feltRekkefolge = [
    'fodselsnummer',
    'input',
    'output',
    'utfall',
    'kilde',
    'sporing',
    'tidsstempel',
    'lovverksversjon',
    'behandlingId',
    'vedtaksperiodeId'
]

function sorterFelter(entries: [string, unknown][]) {
    return [...entries].sort(([a], [b]) => {
        const ia = feltRekkefolge.indexOf(a)
        const ib = feltRekkefolge.indexOf(b)
        if (ia === -1 && ib === -1) return 0
        if (ia === -1) return 1
        if (ib === -1) return -1
        return ia - ib
    })
}

function SubsumsjonRad({ melding }: { melding: Melding }) {
    const data = (melding.subsumsjon ?? melding) as Record<string, unknown>
    // id, eventName, versjon og versjonAvKode skjules — de er ikke relevante å vise i UI-et.
    const { lovverk, paragraf, ledd, bokstav, punktum, id: _id, eventName: _eventName, versjon: _versjon, versjonAvKode: _versjonAvKode, ...resten } = data

    return (
        <Table.ExpandableRow
            content={
                <HGrid columns={1} gap="space-4">
                    {sorterFelter(Object.entries(resten)).map(([nokkel, verdi]) => (
                        <Felt nokkel={nokkel} verdi={verdi} key={nokkel} />
                    ))}
                </HGrid>
            }
        >
            <Table.DataCell textSize="small">
                <BodyShort size="small" textColor="subtle">
                    {visDato(melding.tidsstempel)}
                </BodyShort>
            </Table.DataCell>
            <Table.DataCell textSize="small">
                <BodyShort size="small" textColor="subtle">
                    {visKlokkeslett(melding.tidsstempel)}
                </BodyShort>
            </Table.DataCell>
            <Table.DataCell>{visFelt(lovverk)}</Table.DataCell>
            <Table.DataCell>{visFelt(paragraf)}</Table.DataCell>
            <Table.DataCell>{visFelt(ledd)}</Table.DataCell>
            <Table.DataCell>{visFelt(bokstav)}</Table.DataCell>
            <Table.DataCell>{visFelt(punktum)}</Table.DataCell>
        </Table.ExpandableRow>
    )
}

export function GruppeKort({ gruppe }: { gruppe: Gruppe }) {
    return (
        <Accordion.Item>
            <Accordion.Header>
                <Heading level="3" size="small">
                    vedtaksperiodeId: {gruppe.vedtaksperiodeId} — behandlingId: {gruppe.behandlingId}
                </Heading>
            </Accordion.Header>
            <Accordion.Content>
                <Table size="small">
                    <Table.Header>
                        <Table.Row>
                            <Table.ColumnHeader />
                            <Table.ColumnHeader>Dato</Table.ColumnHeader>
                            <Table.ColumnHeader>Tidspunkt</Table.ColumnHeader>
                            <Table.ColumnHeader>Lovverk</Table.ColumnHeader>
                            <Table.ColumnHeader>Paragraf</Table.ColumnHeader>
                            <Table.ColumnHeader>Ledd</Table.ColumnHeader>
                            <Table.ColumnHeader>Bokstav</Table.ColumnHeader>
                            <Table.ColumnHeader>Punktum</Table.ColumnHeader>
                        </Table.Row>
                    </Table.Header>
                    <Table.Body>
                        {gruppe.meldinger.map((melding) => (
                            <SubsumsjonRad melding={melding} key={melding.id} />
                        ))}
                    </Table.Body>
                </Table>
            </Accordion.Content>
        </Accordion.Item>
    )
}
