package com.limelight.binding.input;

/**
 * Como o trackpad físico está orientado, e quanto vale um passo dele em pixel de tela.
 *
 * Isto morava dentro da Game, misturado com o tratamento de evento, e por isso não tinha
 * teste nenhum — foi assim que o app chegou ao aparelho exigindo que a pessoa trocasse X
 * por Y à mão e ainda pusesse o eixo Y em -100%. As duas coisas juntas são uma rotação de
 * 90°, e uma rotação é aritmética pura: dá para decidir e provar sem Android nenhum.
 *
 * A dedução se apoia num fato do mundo físico: <b>não existe trackpad em pé</b>. Todo pad
 * é mais largo que alto. Se o driver reporta o alcance de Y maior que o de X, ele não está
 * descrevendo um pad estranho — está entregando o pad girado.
 */
public final class TouchpadGeometry {

    /** Passa as unidades do pad adiante. É o que sobra quando não dá para saber a geometria. */
    public static final TouchpadGeometry UNKNOWN = new TouchpadGeometry(false, 1, 1);

    private final boolean rotated;
    private final float scaleX;
    private final float scaleY;

    private TouchpadGeometry(boolean rotated, float scaleX, float scaleY) {
        this.rotated = rotated;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    /**
     * @param padRangeX     alcance bruto do eixo X que o driver reporta
     * @param padRangeY     alcance bruto do eixo Y que o driver reporta
     * @param streamWidth   largura da imagem, em pixel
     * @param streamHeight  altura da imagem, em pixel
     * @param swapOverride  a preferência de trocar eixos, que INVERTE a dedução — serve a
     *                      quem tiver hardware que engane a regra, para não ficar sem saída
     */
    public static TouchpadGeometry resolve(float padRangeX, float padRangeY,
                                           int streamWidth, int streamHeight,
                                           boolean swapOverride) {
        if (padRangeX <= 0 || padRangeY <= 0 || streamWidth <= 0 || streamHeight <= 0) {
            return UNKNOWN;
        }

        boolean rotated = (padRangeY > padRangeX) != swapOverride;

        // Uma varrida inteira do pad tem de dar uma varrida inteira da imagem, para a
        // velocidade não depender da resolução interna do hardware.
        float alongScreenX = rotated ? padRangeY : padRangeX;
        float alongScreenY = rotated ? padRangeX : padRangeY;

        return new TouchpadGeometry(rotated, streamWidth / alongScreenX, streamHeight / alongScreenY);
    }

    /**
     * Escala do eixo horizontal, para quem já resolveu a orientação por fora.
     *
     * A calibração medida no aparelho decide a ORIENTAÇÃO melhor que a dedução daqui, mas
     * a escala continua sendo assunto desta classe: ela é a que sabe o alcance bruto do
     * pad e o tamanho da imagem.
     */
    public float scaleAlongX() {
        return scaleX;
    }

    public float scaleAlongY() {
        return scaleY;
    }

    public boolean isRotated() {
        return rotated;
    }

    /**
     * Quanto o cursor anda na horizontal, dado um passo bruto do pad.
     *
     * Com o pad girado, quem acompanha o dedo indo para a direita é o Y do driver.
     */
    public int screenDeltaX(int padDeltaX, int padDeltaY) {
        return Math.round((rotated ? padDeltaY : padDeltaX) * scaleX);
    }

    /**
     * Quanto o cursor anda na vertical.
     *
     * Com o pad girado é o X do driver, com o sinal trocado — sem a troca de sinal a
     * rotação viraria espelhamento, e o cursor subiria quando o dedo desce.
     */
    public int screenDeltaY(int padDeltaX, int padDeltaY) {
        return Math.round((rotated ? -padDeltaX : padDeltaY) * scaleY);
    }
}
