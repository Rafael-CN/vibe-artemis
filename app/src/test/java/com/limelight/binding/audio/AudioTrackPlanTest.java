package com.limelight.binding.audio;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Prende o layout de canal e a cascata de abertura da saída de áudio.
 *
 * Os dois defeitos que motivaram isto viviam num laço de quatro casos dentro do
 * {@code setup()} do renderer, sem teste nenhum:
 *
 * <ul>
 *   <li>o caso de 4 canais usava CHANNEL_OUT_QUAD, que é FL FR BL BR — mas o Limelight.h
 *       fixa a ordem de saída em FL FR C LFE, ou seja, Centro e LFE iam para as caixas
 *       traseiras;</li>
 *   <li>quando o aparelho recusava a máscara pedida, as quatro tentativas falhavam e o
 *       {@code setup()} devolvia -2 — que o moonlight-common-c trata derrubando a conexão.
 *       O usuário não perdia o surround, perdia o stream.</li>
 * </ul>
 *
 * Aritmética de inteiro, sem Android: os valores de máscara são a fronteira e por isso
 * estão escritos literalmente aqui. Se alguém trocar a construção da máscara, é este
 * arquivo que tem de reprovar.
 */
public class AudioTrackPlanTest {

    private static final int SAMPLE_RATE = 48000;
    private static final int SAMPLES_PER_FRAME = 240;  // 5 ms a 48 kHz

    // --- máscara de canal -------------------------------------------------------------

    @Test
    public void estereo_usaOsDoisCanaisDaFrente() {
        // AudioFormat.CHANNEL_OUT_STEREO
        assertEquals(0xC, AudioTrackPlan.channelMaskFor(2));
    }

    @Test
    public void quatroCanais_saoFlFrCentroLfe_naoAsTraseiras() {
        // Este é o defeito inteiro. CHANNEL_OUT_QUAD é 0xCC (FL FR BL BR); a ordem do
        // Limelight.h com 4 canais é FL FR C LFE, que dá 0x3C.
        assertEquals(0x3C, AudioTrackPlan.channelMaskFor(4));
    }

    @Test
    public void cincoPontoUm_bateComOValorDoAndroid() {
        // AudioFormat.CHANNEL_OUT_5POINT1
        assertEquals(0xFC, AudioTrackPlan.channelMaskFor(6));
    }

    @Test
    public void setePontoUm_bateComAConstanteQueEstavaChumbada() {
        // O renderer trazia 0x000018fc escrito à mão com o comentário
        // "AudioFormat.CHANNEL_OUT_7POINT1_SURROUND". A construção bit a bit tem de dar
        // exatamente o mesmo número, senão o surround sai com os canais trocados.
        assertEquals(0x18FC, AudioTrackPlan.channelMaskFor(8));
    }

    @Test
    public void cadaCanalNovoSoAcrescentaBit() {
        // A ordem de intercalação do Android é a ordem crescente de bit. Se a máscara de N
        // canais não contiver inteira a de N-1, algum canal mudou de posição no meio.
        for (int n = 3; n <= 8; n++) {
            int menor = AudioTrackPlan.channelMaskFor(n - 1);
            int maior = AudioTrackPlan.channelMaskFor(n);
            assertEquals("máscara de " + n + " canais tem de conter a de " + (n - 1),
                    menor, maior & menor);
            assertEquals("cada canal novo acrescenta exatamente um bit",
                    n, Integer.bitCount(maior));
        }
    }

    @Test
    public void contagemForaDoProtocolo_naoTemMascara() {
        assertEquals(AudioTrackPlan.NO_CHANNEL_MASK, AudioTrackPlan.channelMaskFor(0));
        assertEquals(AudioTrackPlan.NO_CHANNEL_MASK, AudioTrackPlan.channelMaskFor(1));
        assertEquals(AudioTrackPlan.NO_CHANNEL_MASK, AudioTrackPlan.channelMaskFor(9));
        assertEquals(AudioTrackPlan.NO_CHANNEL_MASK, AudioTrackPlan.channelMaskFor(-1));
    }

    // --- cascata de tentativas --------------------------------------------------------

    private static AudioTrackPlan estereo(int nativeRate, int minBuffer, boolean audioFx,
                                          boolean indexado) {
        return AudioTrackPlan.resolve(2, SAMPLES_PER_FRAME, SAMPLE_RATE,
                nativeRate, minBuffer, audioFx, indexado);
    }

    @Test
    public void semObstaculo_tentaBaixaLatenciaPrimeiro() {
        AudioTrackPlan plan = estereo(SAMPLE_RATE, 3840, false, false);
        assertNotNull(plan);

        List<AudioTrackPlan.Attempt> attempts = plan.attempts();
        assertEquals(4, attempts.size());

        // 2 canais * 240 samples * 2 bytes = 960 bytes por frame; o buffer curto é 2 frames.
        assertEquals(960, plan.bytesPerFrame());

        assertTrue(attempts.get(0).lowLatency);
        assertEquals(1920, attempts.get(0).bufferSizeInBytes);
        assertTrue(attempts.get(1).lowLatency);
        assertEquals(3840, attempts.get(1).bufferSizeInBytes);
        assertFalse(attempts.get(2).lowLatency);
        assertEquals(1920, attempts.get(2).bufferSizeInBytes);
        assertFalse(attempts.get(3).lowLatency);
        assertEquals(3840, attempts.get(3).bufferSizeInBytes);
    }

    @Test
    public void comEfeitosLigados_nenhumaTentativaPedeBaixaLatencia() {
        // O pipeline de efeitos do sistema é incompatível com o modo de baixa latência.
        // Pedir os dois devolve uma track que falha na criação e queima duas tentativas.
        AudioTrackPlan plan = estereo(SAMPLE_RATE, 3840, true, false);
        assertNotNull(plan);

        assertEquals(2, plan.attempts().size());
        for (AudioTrackPlan.Attempt attempt : plan.attempts()) {
            assertFalse("efeitos ligados proíbem baixa latência", attempt.lowLatency);
        }
    }

    @Test
    public void comTaxaNativaDiferente_nenhumaTentativaPedeBaixaLatencia() {
        // É o caso do fone Bluetooth: a saída passa a rodar a 44,1 kHz e o resampler do
        // sistema entra no caminho, anulando o ganho e transformando o buffer de dois
        // frames em underrun contínuo.
        AudioTrackPlan plan = estereo(44100, 3840, false, false);
        assertNotNull(plan);

        assertEquals(2, plan.attempts().size());
        for (AudioTrackPlan.Attempt attempt : plan.attempts()) {
            assertFalse("taxa nativa diferente proíbe baixa latência", attempt.lowLatency);
        }
    }

    @Test
    public void bufferGrande_eArredondadoParaFrameInteiro() {
        AudioTrackPlan plan = estereo(SAMPLE_RATE, 3841, false, false);
        assertNotNull(plan);

        // 3841 não é múltiplo de 960; o próximo frame inteiro é 4800.
        assertEquals(4800, plan.attempts().get(1).bufferSizeInBytes);
        assertEquals(0, plan.attempts().get(1).bufferSizeInBytes % plan.bytesPerFrame());
    }

    @Test
    public void minimoNegativo_naoViraBufferNegativo() {
        // getMinBufferSize devolve ERROR (-1) ou ERROR_BAD_VALUE (-2) quando recusa a
        // combinação. Passar isso adiante criaria uma AudioTrack com buffer negativo.
        for (int recusa : new int[] { -1, -2 }) {
            AudioTrackPlan plan = estereo(SAMPLE_RATE, recusa, false, false);
            assertNotNull(plan);

            for (AudioTrackPlan.Attempt attempt : plan.attempts()) {
                assertTrue("buffer tem de ser positivo mesmo com mínimo recusado",
                        attempt.bufferSizeInBytes > 0);
                assertEquals(0, attempt.bufferSizeInBytes % plan.bytesPerFrame());
            }
        }
    }

    // --- saída de emergência por máscara de índice ------------------------------------

    @Test
    public void surround_ganhaUmaUltimaTentativaPorIndice() {
        AudioTrackPlan plan = AudioTrackPlan.resolve(6, SAMPLES_PER_FRAME, SAMPLE_RATE,
                SAMPLE_RATE, 8192, false, true);
        assertNotNull(plan);

        List<AudioTrackPlan.Attempt> attempts = plan.attempts();
        AudioTrackPlan.Attempt ultima = attempts.get(attempts.size() - 1);

        assertTrue("a última carta é a máscara por índice", ultima.indexedMask);
        assertEquals("seis canais quaisquer", 0x3F, ultima.channelMask);
        assertFalse("a saída de emergência não pede baixa latência", ultima.lowLatency);

        // E só a última: as anteriores continuam sendo posicionais, que é o que faz o
        // aparelho rotear cada canal para a caixa certa quando ele entende o layout.
        for (int i = 0; i < attempts.size() - 1; i++) {
            assertFalse(attempts.get(i).indexedMask);
            assertEquals(0xFC, attempts.get(i).channelMask);
        }
    }

    @Test
    public void semSuporteAIndice_naoHaTentativaPorIndice() {
        AudioTrackPlan plan = AudioTrackPlan.resolve(6, SAMPLES_PER_FRAME, SAMPLE_RATE,
                SAMPLE_RATE, 8192, false, false);
        assertNotNull(plan);

        for (AudioTrackPlan.Attempt attempt : plan.attempts()) {
            assertFalse("setChannelIndexMask não existe antes da API 24",
                    attempt.indexedMask);
        }
    }

    @Test
    public void estereo_naoPrecisaDeSaidaDeEmergencia() {
        // Aparelho que recusa estéreo não tem saída de áudio nenhuma; gastar uma tentativa
        // ali só atrasaria o diagnóstico.
        AudioTrackPlan plan = estereo(SAMPLE_RATE, 3840, false, true);
        assertNotNull(plan);

        for (AudioTrackPlan.Attempt attempt : plan.attempts()) {
            assertFalse(attempt.indexedMask);
        }
    }

    @Test
    public void parametrosImpossiveis_naoDaoPlano() {
        assertNull(AudioTrackPlan.resolve(3000, SAMPLES_PER_FRAME, SAMPLE_RATE,
                SAMPLE_RATE, 3840, false, true));
        assertNull(AudioTrackPlan.resolve(2, 0, SAMPLE_RATE,
                SAMPLE_RATE, 3840, false, true));
    }

    @Test
    public void planoGuardaOQueOChamadorPrecisaParaRefazerAConta() {
        AudioTrackPlan plan = AudioTrackPlan.resolve(8, 480, SAMPLE_RATE,
                SAMPLE_RATE, 16384, false, true);
        assertNotNull(plan);

        assertEquals(8, plan.channelCount());
        assertEquals(8 * 480 * 2, plan.bytesPerFrame());
    }

    // --- quando recriar a saída -------------------------------------------------------

    @Test
    public void erroDeObjetoMorto_pedeRecriacao() {
        // AudioTrack.ERROR_DEAD_OBJECT. É o retorno quando o servidor de áudio reinicia ou
        // quando a track é invalidada numa troca de rota — o caminho pelo qual o áudio
        // sumia até o fim da sessão, porque o retorno do write era ignorado.
        assertTrue(AudioTrackPlan.requiresRebuild(-6));
    }

    @Test
    public void operacaoInvalida_pedeRecriacao() {
        // AudioTrack.ERROR_INVALID_OPERATION: track em estado inválido, só se resolve
        // recriando.
        assertTrue(AudioTrackPlan.requiresRebuild(-3));
    }

    @Test
    public void argumentoRuimEEscritaNormal_naoPedemRecriacao() {
        assertFalse("ERROR_BAD_VALUE é argumento errado nosso; recriar não conserta",
                AudioTrackPlan.requiresRebuild(-2));
        assertFalse("ERROR genérico não identifica track morta",
                AudioTrackPlan.requiresRebuild(-1));
        assertFalse("nada escrito não é track morta", AudioTrackPlan.requiresRebuild(0));
        assertFalse("escrita bem-sucedida", AudioTrackPlan.requiresRebuild(480));
    }
}
