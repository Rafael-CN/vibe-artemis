package com.limelight.binding.input;

/**
 * Como o movimento do dedo no trackpad vira movimento do cursor.
 *
 * Eu tentei DEDUZIR isso duas vezes e errei as duas. A primeira tentativa usava "um
 * trackpad é sempre mais largo que alto, então alcance de Y maior que o de X significa pad
 * girado". O aparelho do dono desmentiu a premissa: a capa do S8 Ultra reporta
 * X [0..2959] e Y [0..1847], que é a RESOLUÇÃO DA TELA — o pad é mapeado em absoluto sobre
 * o display, então ele sempre se anuncia deitado e a regra nunca dispara, por mais girado
 * que o movimento chegue.
 *
 * Não dá para deduzir de fora o que o driver faz por dentro. Então isto aqui não deduz:
 * mede. Dois gestos conhecidos — um para a direita, um para baixo — dizem exatamente qual
 * eixo do pad acompanha qual eixo da tela e com que sinal, e a partir daí a conta é fixa.
 *
 * As oito combinações possíveis (quatro rotações, com ou sem espelho) cabem todas na mesma
 * matriz de dois por dois com entradas em {-1, 0, 1}: em qualquer orientação sã, um eixo
 * da tela acompanha exatamente um eixo do pad.
 */
public final class TouchpadCalibration {

    /** Sem calibração: o pad já está na orientação da tela. */
    public static final TouchpadCalibration IDENTITY = new TouchpadCalibration(1, 0, 0, 1);

    /** Movimento abaixo disto é ruído de dedo parado, não gesto. */
    private static final float MIN_GESTURE = 8f;

    private final int xFromPadX, xFromPadY;
    private final int yFromPadX, yFromPadY;

    private TouchpadCalibration(int xFromPadX, int xFromPadY, int yFromPadX, int yFromPadY) {
        this.xFromPadX = xFromPadX;
        this.xFromPadY = xFromPadY;
        this.yFromPadX = yFromPadX;
        this.yFromPadY = yFromPadY;
    }

    /**
     * Monta a calibração a partir de dois gestos medidos.
     *
     * @param rightPadDx deslocamento em X que o pad reportou quando o dedo foi para a DIREITA
     * @param rightPadDy deslocamento em Y no mesmo gesto
     * @param downPadDx  deslocamento em X quando o dedo foi para BAIXO
     * @param downPadDy  deslocamento em Y no mesmo gesto
     * @return a calibração, ou null se algum dos gestos foi curto ou ambíguo demais
     */
    public static TouchpadCalibration fromGestures(float rightPadDx, float rightPadDy,
                                                   float downPadDx, float downPadDy) {
        int[] paraDireita = eixoDominante(rightPadDx, rightPadDy);
        int[] paraBaixo = eixoDominante(downPadDx, downPadDy);
        if (paraDireita == null || paraBaixo == null) {
            return null;
        }

        // Os dois gestos são perpendiculares na tela; se caíram no mesmo eixo do pad, uma
        // das medições foi torta e aceitar isso daria um cursor preso numa linha.
        if (paraDireita[0] == paraBaixo[0]) {
            return null;
        }

        return new TouchpadCalibration(
                paraDireita[0] == 0 ? paraDireita[1] : 0,
                paraDireita[0] == 1 ? paraDireita[1] : 0,
                paraBaixo[0] == 0 ? paraBaixo[1] : 0,
                paraBaixo[0] == 1 ? paraBaixo[1] : 0);
    }

    /** @return {eixo (0=X, 1=Y), sinal}, ou null se o gesto foi curto ou diagonal demais. */
    private static int[] eixoDominante(float dx, float dy) {
        float ax = Math.abs(dx);
        float ay = Math.abs(dy);

        if (Math.max(ax, ay) < MIN_GESTURE) {
            return null;
        }

        // Um gesto reto tem um eixo claramente maior. Metade é folga generosa para mão
        // humana; acima disso não dá para saber qual eixo a pessoa quis mover.
        if (Math.min(ax, ay) > Math.max(ax, ay) * 0.5f) {
            return null;
        }

        if (ax >= ay) {
            return new int[] { 0, dx >= 0 ? 1 : -1 };
        }
        return new int[] { 1, dy >= 0 ? 1 : -1 };
    }

    /** Reconstrói do que foi guardado. Devolve IDENTITY se o texto não for reconhecido. */
    public static TouchpadCalibration parse(String stored) {
        if (stored == null) {
            return IDENTITY;
        }
        String[] partes = stored.split(",");
        if (partes.length != 4) {
            return IDENTITY;
        }
        try {
            int a = Integer.parseInt(partes[0].trim());
            int b = Integer.parseInt(partes[1].trim());
            int c = Integer.parseInt(partes[2].trim());
            int d = Integer.parseInt(partes[3].trim());
            // Uma matriz sã tem exatamente um valor não nulo por linha e por coluna.
            if (Math.abs(a) + Math.abs(b) != 1 || Math.abs(c) + Math.abs(d) != 1
                    || Math.abs(a) + Math.abs(c) != 1) {
                return IDENTITY;
            }
            return new TouchpadCalibration(a, b, c, d);
        } catch (NumberFormatException e) {
            return IDENTITY;
        }
    }

    public String serialize() {
        return xFromPadX + "," + xFromPadY + "," + yFromPadX + "," + yFromPadY;
    }

    public boolean isIdentity() {
        return xFromPadX == 1 && xFromPadY == 0 && yFromPadX == 0 && yFromPadY == 1;
    }

    public float screenDeltaX(float padDx, float padDy) {
        return xFromPadX * padDx + xFromPadY * padDy;
    }

    public float screenDeltaY(float padDx, float padDy) {
        return yFromPadX * padDx + yFromPadY * padDy;
    }
}
