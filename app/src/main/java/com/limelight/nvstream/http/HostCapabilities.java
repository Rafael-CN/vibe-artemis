package com.limelight.nvstream.http;

/**
 * O que ESTE host sabe fazer, perguntado antes de oferecer e não descoberto depois de
 * falhar.
 *
 * <p>A distinção entre GFE, Sunshine, os forks do Sunshine e o Apollo estava espalhada em
 * heurísticas soltas — o codinome MJOLNIR no estado, o quarto componente negativo da
 * versão no lado nativo, a presença de campos do Apollo no serverinfo. No caso do
 * clipboard não estava em lugar nenhum: o cliente mandava o POST e lia a resposta para
 * saber se o endpoint existia ("the 200ed 404 from Sunshine", em
 * {@link NvHTTP#sendClipboard(String)}). Uma ação que só descobre que não existe DEPOIS de
 * tentar não tem como decidir se aparece na tela — e era esse o sintoma: no Vibeshine as
 * duas ações de área de transferência apareciam no menu e não funcionavam.
 *
 * <p>O modelo de permissões é extensão do <b>Apollo</b>. Host que não o implementa —
 * Sunshine vanilla e o Vibeshine, que é o host deste fork — não manda o campo
 * {@code Permission} no serverinfo, e a máscara fica em {@link #PERMISSION_UNKNOWN}.
 * Ausência é resposta: sem modelo de permissões não há as operações que ele governa.
 * O padrão é não oferecer nada; liga só o que o host anuncia.
 *
 * <p>Cuidado que esta classe existe para não deixar escapar: {@link #PERMISSION_UNKNOWN}
 * é {@code -1}, ou seja, <i>todos</i> os bits ligados. Testar bit direto na máscara
 * responde "sim" para todo host que não tem modelo de permissões — exatamente o contrário
 * do certo. Por isso toda consulta aqui passa por {@link #hasPermissionModel()} antes de
 * olhar o bit, e nenhum chamador de fora vê a máscara crua.
 */
public final class HostCapabilities {

    /**
     * Nenhum modelo de permissões — o host não mandou o campo, ou mandou algo que não é
     * número. Mesmo valor que {@link ComputerDetails#permission} nasce tendo, para que um
     * host ainda não sondado e um host sem o campo signifiquem a mesma coisa: não sabemos,
     * logo não oferecemos.
     */
    public static final int PERMISSION_UNKNOWN = -1;

    /*
     * Os bits do enum PERM do Apollo. Estavam como literais hexadecimais espalhados por
     * ComputerDetails.toString(), com o enum transcrito ali do lado em comentário; a
     * tabela passa a ter um dono só, e os valores saem dos deslocamentos do próprio enum
     * em vez de serem contados à mão em hexa.
     *
     * Transcrição do host — única fonte que temos, e que envelhece se o Apollo mexer no
     * enum:
     *
     *     enum class PERM: uint32_t {
     *         _reserved        = 1,
     *
     *         _input           = _reserved << 8,   // Input permission group
     *         input_controller = _input << 0,      // Allow controller input
     *         input_touch      = _input << 1,      // Allow touch input
     *         input_pen        = _input << 2,      // Allow pen input
     *         input_mouse      = _input << 3,      // Allow mouse input
     *         input_kbd        = _input << 4,      // Allow keyboard input
     *         _all_inputs      = input_controller | input_touch | input_pen | input_mouse | input_kbd,
     *
     *         _operation       = _input << 8,      // Operation permission group
     *         clipboard_set    = _operation << 0,  // Allow set clipboard from client
     *         clipboard_read   = _operation << 1,  // Allow read clipboard from host
     *         file_upload      = _operation << 2,  // Allow upload files to host
     *         file_dwnload     = _operation << 3,  // Allow download files from host
     *         server_cmd       = _operation << 4,  // Allow execute server cmd
     *         _all_opeiations  = clipboard_set | clipboard_read | file_upload | file_dwnload | server_cmd,
     *
     *         _action          = _operation << 8,  // Action permission group
     *         list             = _action << 0,     // Allow list apps
     *         view             = _action << 1,     // Allow view streams
     *         launch           = _action << 2,     // Allow launch apps
     *         _allow_view      = view | launch,    // Launch contains view permission
     *         _all_actions     = list | view | launch,
     *
     *         _default         = view | list,      // Default permissions for new clients
     *         _no              = 0,                // No permissions are granted
     *         _all             = _all_inputs | _all_opeiations | _all_actions,
     *     };
     */
    private static final int PERM_RESERVED = 1;

    private static final int PERM_GROUP_INPUT = PERM_RESERVED << 8;
    public static final int PERM_INPUT_CONTROLLER = PERM_GROUP_INPUT;
    public static final int PERM_INPUT_TOUCH = PERM_GROUP_INPUT << 1;
    public static final int PERM_INPUT_PEN = PERM_GROUP_INPUT << 2;
    public static final int PERM_INPUT_MOUSE = PERM_GROUP_INPUT << 3;
    public static final int PERM_INPUT_KEYBOARD = PERM_GROUP_INPUT << 4;

    private static final int PERM_GROUP_OPERATION = PERM_GROUP_INPUT << 8;
    /** Deixa o cliente ESCREVER na área de transferência do host. */
    public static final int PERM_CLIPBOARD_SET = PERM_GROUP_OPERATION;
    /** Deixa o cliente LER a área de transferência do host. */
    public static final int PERM_CLIPBOARD_READ = PERM_GROUP_OPERATION << 1;
    public static final int PERM_FILE_UPLOAD = PERM_GROUP_OPERATION << 2;
    public static final int PERM_FILE_DOWNLOAD = PERM_GROUP_OPERATION << 3;
    public static final int PERM_SERVER_CMD = PERM_GROUP_OPERATION << 4;

    private static final int PERM_GROUP_ACTION = PERM_GROUP_OPERATION << 8;
    public static final int PERM_LIST_APPS = PERM_GROUP_ACTION;
    public static final int PERM_VIEW_STREAMS = PERM_GROUP_ACTION << 1;
    public static final int PERM_LAUNCH_APPS = PERM_GROUP_ACTION << 2;

    private static final HostCapabilities UNKNOWN = new HostCapabilities(PERMISSION_UNKNOWN);

    private final int permissionMask;

    private HostCapabilities(int permissionMask) {
        this.permissionMask = permissionMask;
    }

    /** Host que ainda não respondeu, ou que não tem o que responder. Não habilita nada. */
    public static HostCapabilities unknown() {
        return UNKNOWN;
    }

    /**
     * Toda máscara negativa colapsa em {@link #unknown()} — não só o {@code -1}. O campo
     * chega do host como texto e é convertido em {@code NvHTTP.getComputerDetails}; se um
     * dia vier algo que o parse leia como negativo, o resultado tem que ser "não sei", e
     * nunca "pode tudo".
     */
    public static HostCapabilities fromPermissionMask(int permissionMask) {
        return permissionMask < 0 ? UNKNOWN : new HostCapabilities(permissionMask);
    }

    /**
     * A máscara como veio, só para quem precisa carregá-la de um processo a outro (o
     * Intent da sessão) ou mostrá-la em diagnóstico. Para decidir comportamento, use as
     * consultas — elas já tratam o caso de não haver modelo nenhum.
     */
    public int getPermissionMask() {
        return permissionMask;
    }

    /** O host anunciou permissões, então o silêncio dele sobre um bit quer dizer "não". */
    public boolean hasPermissionModel() {
        return permissionMask >= 0;
    }

    private boolean allows(int bit) {
        return hasPermissionModel() && (permissionMask & bit) != 0;
    }

    /** Mandar o que está copiado no aparelho para o PC. */
    public boolean supportsClipboardSend() {
        return allows(PERM_CLIPBOARD_SET);
    }

    /** Trazer o que está copiado no PC para o aparelho. */
    public boolean supportsClipboardFetch() {
        return allows(PERM_CLIPBOARD_READ);
    }

    // As consultas param aqui de propósito: só existe método para o que alguém pergunta
    // hoje. A tabela de bits acima está completa porque é transcrição de um contrato — um
    // buraco nela faria o próximo recontar deslocamento na mão —, mas consulta sem
    // chamador é peso morto, e este fork nasceu de fugir de peso morto. Os próximos
    // consumidores (a sincronização automática de clipboard em Game, os comandos de
    // servidor) acrescentam a sua linha quando chegarem.
}
