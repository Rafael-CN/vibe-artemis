@echo off
setlocal
rem ============================================================================
rem  PAREAR UM APARELHO POR WI-FI (celular ou tablet) - VibeArtemis
rem
rem  No aparelho: Configuracoes > Opcoes do desenvolvedor > Depuracao por Wi-Fi
rem    1. ligue "Depuracao por Wi-Fi"
rem    2. toque em "Parear dispositivo com codigo de pareamento"
rem       -> aparece um CODIGO de 6 digitos e um "Endereco IP e porta" (ex. 192.168.0.23:37123)
rem    3. na tela principal da Depuracao por Wi-Fi ha OUTRO "Endereco IP e porta"
rem       (a porta de CONEXAO, diferente da de pareamento)
rem  Rode este arquivo e responda as tres perguntas. Depois disso, todo build
rem  passa a ser instalado e fotografado direto no aparelho, sem cabo.
rem ============================================================================

set /p PAR=Endereco IP:porta de PAREAMENTO (o que aparece junto do codigo):
set /p CODIGO=Codigo de pareamento (6 digitos):
set /p CON=Endereco IP:porta de CONEXAO (o da tela principal da Depuracao por Wi-Fi):

echo.
echo [1/2] Pareando com %PAR% ...
adb pair %PAR% %CODIGO%
if errorlevel 1 (
    echo   Falhou. Confira se o celular e o PC estao na mesma rede Wi-Fi e se o codigo ainda esta na tela.
    pause
    exit /b 1
)

echo [2/2] Conectando em %CON% ...
adb connect %CON%
echo.
adb devices -l
echo.
echo Se o aparelho aparece acima como "device", esta pronto. O pareamento fica salvo;
echo nas proximas vezes so "adb connect %CON%" (ou este arquivo de novo) basta.
pause
