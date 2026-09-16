import { type FormEvent, useState } from 'react'
import { Accordion, Alert, BodyShort, Button, Heading, Page, TextField, ToggleGroup, VStack } from '@navikt/ds-react'
import { ApiError, hentMeldinger, hentMeldingerForFodselsnummer } from './api'
import { GruppeKort } from './GruppeKort'
import { grupperMeldinger } from './types'

type Søketype = 'vedtaksperiodeId' | 'fodselsnummer'

function App() {
    const [søketype, setSøketype] = useState<Søketype>('vedtaksperiodeId')
    const [søkeverdi, setSøkeverdi] = useState('')
    const [status, setStatus] = useState('')
    const [feil, setFeil] = useState(false)
    const [grupper, setGrupper] = useState<ReturnType<typeof grupperMeldinger>>([])

    async function sok(event: FormEvent) {
        event.preventDefault()
        setGrupper([])
        setFeil(false)
        setStatus('Søker …')

        try {
            const meldinger =
                søketype === 'vedtaksperiodeId'
                    ? await hentMeldinger(søkeverdi.trim())
                    : await hentMeldingerForFodselsnummer(søkeverdi.trim())
            if (meldinger.length === 0) {
                setStatus('Fant ingen meldinger.')
                return
            }
            const nyeGrupper = grupperMeldinger(meldinger)
            const antallVedtaksperioder = new Set(nyeGrupper.map((gruppe) => gruppe.vedtaksperiodeId)).size
            setStatus(`${antallVedtaksperioder} vedtaksperiode(r), ${meldinger.length} subsumsjon(er) funnet.`)
            setGrupper(nyeGrupper)
        } catch (err) {
            setFeil(true)
            if (err instanceof ApiError) {
                setStatus(`Feil: ${err.status} ${err.message}`)
            } else {
                setStatus(`Uventet feil: ${String(err)}`)
            }
        }
    }

    return (
        <Page>
            <Page.Block width="lg" gutters>
                <VStack gap="space-6" paddingBlock="space-8 space-4">
                    <Heading level="1" size="large">
                        Sparkiv-subsumsjon
                    </Heading>
                    <BodyShort>Søk opp meldinger for en vedtaksperiode eller et fødselsnummer.</BodyShort>

                    <form onSubmit={sok}>
                        <VStack gap="space-4" align="start">
                            <ToggleGroup
                                value={søketype}
                                onChange={(value) => {
                                    setSøketype(value as Søketype)
                                    setSøkeverdi('')
                                }}
                            >
                                <ToggleGroup.Item value="vedtaksperiodeId">vedtaksperiodeId</ToggleGroup.Item>
                                <ToggleGroup.Item value="fodselsnummer">Fødselsnummer</ToggleGroup.Item>
                            </ToggleGroup>
                            <TextField
                                label={søketype === 'vedtaksperiodeId' ? 'vedtaksperiodeId' : 'Fødselsnummer'}
                                id="sok"
                                placeholder={søketype === 'vedtaksperiodeId' ? 'UUID' : '11 siffer'}
                                required
                                value={søkeverdi}
                                onChange={(event) => setSøkeverdi(event.target.value)}
                            />
                            <Button type="submit">Søk</Button>
                        </VStack>
                    </form>

                    {status && (
                        <Alert variant={feil ? 'error' : 'info'} inline>
                            {status}
                        </Alert>
                    )}

                    <Accordion>
                        {grupper.map((gruppe) => (
                            <GruppeKort gruppe={gruppe} key={`${gruppe.vedtaksperiodeId}|${gruppe.behandlingId}`} />
                        ))}
                    </Accordion>
                </VStack>
            </Page.Block>
        </Page>
    )
}

export default App
