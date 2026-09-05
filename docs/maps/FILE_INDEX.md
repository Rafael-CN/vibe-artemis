<!-- GERADO POR tools/codemap/codemap.mjs — NÃO EDITE À MÃO. Rode: node tools/codemap/codemap.mjs -->

# Índice de Arquivos

> Um registro por arquivo de código. Use para localizar o arquivo certo **sem abri-lo**. Colunas: linhas, tipos declarados, métodos públicos relevantes. Rank = PageRank no grafo de imports (quão central o arquivo é).

Total: **291 arquivos**, **85.848 linhas**, **337 tipos**, **3138 métodos**.

## `app/src/main/java/com/limelight/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`Game.java`](../../app/src/main/java/com/limelight/Game.java) | 7054 | 13.0 | Game, GameMenuCallbacks, Voltar | `onServiceConnected()`:336 `onServiceDisconnected()`:345 `sendText()`:434 `sendBackspaces()`:441 `onCreate()`:463 `onCapturedPointer()`:659 `notifyCrash()`:790 `aoComecarAAbrir()`:1071 |
| [`CascaDoApp.java`](../../app/src/main/java/com/limelight/CascaDoApp.java) | 1551 | 2.5 | CascaDoApp, ComputerObject | `dizerQueHaSessao()`:178 `onServiceConnected()`:201 `run()`:208 `run()`:225 `onServiceDisconnected()`:239 `aoTrocarConfiguracao()`:244 `criar()`:491 `onSurfaceCreated()`:505 |
| [`CatalogoDoLugar.java`](../../app/src/main/java/com/limelight/CatalogoDoLugar.java) | 976 | 4.2 | CatalogoDoLugar | `ligarBinder()`:108 `ligarAncora()`:118 `run()`:140 `run()`:243 `run()`:275 `marcarRecemPareado()`:286 `aoTrocarConfiguracao()`:290 `ouvinte()`:316 |
| [`EditProfileActivity.java`](../../app/src/main/java/com/limelight/EditProfileActivity.java) | 700 | 4.0 | EditProfileActivity, ProfilePreferenceFragment, InMemoryPreferenceDataStore, InMemorySharedPreferences, InMemoryEditor | `onCreate()`:58 `onCreateOptionsMenu()`:238 `onOptionsItemSelected()`:250 `getInMemoryPrefs()`:400 `getPrefs()`:436 `getPrefs()`:460 `onCreateView()`:470 `onCreatePreferences()`:475 |
| [`GameMenu.java`](../../app/src/main/java/com/limelight/GameMenu.java) | 549 | 10.0 | GameMenu, MenuOption | `toString()`:85 `getView()`:168 `onGlobalLayout()`:230 `onGlobalLayout()`:276 `showSpecialKeysMenu()`:300 `showMenu()`:417 `hideMenu()`:537 `isMenuOpen()`:545 |
| [`ShortcutTrampoline.java`](../../app/src/main/java/com/limelight/ShortcutTrampoline.java) | 538 | 3.1 | ShortcutTrampoline | `onServiceConnected()`:62 `run()`:69 `notifyComputerUpdated()`:104 `run()`:130 `run()`:163 `run()`:174 `onServiceDisconnected()`:244 `validateHostInput()`:249 |
| [`PosterContentProvider.java`](../../app/src/main/java/com/limelight/PosterContentProvider.java) | 153 | 3.1 | PosterContentProvider | `openFile()`:45 `openBoxArtFile()`:55 `delete()`:109 `getType()`:114 `insert()`:119 `onCreate()`:124 `query()`:130 `update()`:136 |
| [`ProfilesActivity.java`](../../app/src/main/java/com/limelight/ProfilesActivity.java) | 125 | 4.3 | ProfilesActivity | `onCreate()`:29 `onDestroy()`:115 `onProfilesChanged()`:121 |
| [`HelpActivity.java`](../../app/src/main/java/com/limelight/HelpActivity.java) | 119 | 6.6 | HelpActivity | `onCreate()`:25 `onBackInvoked()`:31 `onPageStarted()`:57 `onPageFinished()`:68 `onDestroy()`:96 `onBackPressed()`:108 |
| [`DebugInfoActivity.java`](../../app/src/main/java/com/limelight/DebugInfoActivity.java) | 111 | 4.9 | DebugInfoActivity | `onCreate()`:44 |
| [`PcView.java`](../../app/src/main/java/com/limelight/PcView.java) | 102 | 3.8 | PcView | `onCreate()`:36 `onResume()`:56 `onPause()`:62 `onStop()`:68 `onDestroy()`:74 `onConfigurationChanged()`:80 `onCreateContextMenu()`:86 `onContextMenuClosed()`:92 |
| [`KeyboardAccessibilityService.java`](../../app/src/main/java/com/limelight/KeyboardAccessibilityService.java) | 79 | 2.5 | KeyboardAccessibilityService | `onKeyEvent()`:23 `onServiceConnected()`:59 `onAccessibilityEvent()`:71 `onInterrupt()`:75 |
| [`StartExternalDisplayControlReceiver.java`](../../app/src/main/java/com/limelight/StartExternalDisplayControlReceiver.java) | 58 | 3.4 | StartExternalDisplayControlReceiver | `onReceive()`:25 |
| [`SensitivityBean.java`](../../app/src/main/java/com/limelight/SensitivityBean.java) | 49 | 2.5 | SensitivityBean | `getLastAbsoluteX()`:17 `setLastAbsoluteX()`:21 `getLastAbsoluteY()`:25 `setLastAbsoluteY()`:29 `getLastRelativelyX()`:33 `setLastRelativelyX()`:37 `getLastRelativelyY()`:41 `setLastRelativelyY()`:45 |
| [`LimeLog.java`](../../app/src/main/java/com/limelight/LimeLog.java) | 26 | 77.1 | LimeLog | — |
| [`ArtemisApplication.java`](../../app/src/main/java/com/limelight/ArtemisApplication.java) | 18 | 2.5 | ArtemisApplication | `onCreate()`:11 |

## `app/src/main/java/com/limelight/binding/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`PlatformBinding.java`](../../app/src/main/java/com/limelight/binding/PlatformBinding.java) | 15 | 4.0 | PlatformBinding | — |

## `app/src/main/java/com/limelight/binding/audio/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`AndroidAudioRenderer.java`](../../app/src/main/java/com/limelight/binding/audio/AndroidAudioRenderer.java) | 474 | 3.6 | AndroidAudioRenderer, RouteWatcher | `setup()`:139 `playDecodedAudio()`:279 `start()`:334 `stop()`:343 `cleanup()`:352 `onAudioDevicesAdded()`:459 `onAudioDevicesRemoved()`:469 |
| [`AudioTrackPlan.java`](../../app/src/main/java/com/limelight/binding/audio/AudioTrackPlan.java) | 182 | 2.5 | AudioTrackPlan, Attempt | `toString()`:70 `attempts()`:157 `channelCount()`:161 `bytesPerFrame()`:165 |

## `app/src/main/java/com/limelight/binding/crypto/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`AndroidCryptoProvider.java`](../../app/src/main/java/com/limelight/binding/crypto/AndroidCryptoProvider.java) | 260 | 3.5 | AndroidCryptoProvider | `getClientCertificate()`:191 `getClientPrivateKey()`:218 `getPemEncodedClientCertificate()`:245 `encodeBase64String()`:256 |

## `app/src/main/java/com/limelight/binding/input/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`ControllerHandler.java`](../../app/src/main/java/com/limelight/binding/input/ControllerHandler.java) | 3474 | 4.4 | ControllerHandler, GenericControllerContext, InputDeviceContext, UsbDeviceContext | `hasController()`:229 `onInputDeviceAdded()`:234 `onInputDeviceRemoved()`:239 `onInputDeviceChanged()`:252 `stop()`:272 `destroy()`:296 `disableSensors()`:305 `enableSensors()`:312 |
| [`KeyboardTranslator.java`](../../app/src/main/java/com/limelight/binding/input/KeyboardTranslator.java) | 455 | 3.9 | KeyboardTranslator, KeyboardMapping | `getDeviceKeyCodeForQwertyKeyCode()`:134 `getQwertyKeyCodeForDeviceKeyCode()`:138 `hasNormalizedMapping()`:161 `translate()`:183 `onInputDeviceAdded()`:429 `onInputDeviceRemoved()`:439 `onInputDeviceChanged()`:444 |
| [`TextInputPump.java`](../../app/src/main/java/com/limelight/binding/input/TextInputPump.java) | 200 | 2.7 | TextInputPump, Sink, Scheduler | `run()`:66 `run()`:83 `offerText()`:105 `offerBackspaces()`:133 `runWhenIdle()`:151 `clear()`:180 `isIdle()`:188 `pendingChunks()`:192 |
| [`TouchpadCalibration.java`](../../app/src/main/java/com/limelight/binding/input/TouchpadCalibration.java) | 131 | 3.5 | TouchpadCalibration | `serialize()`:115 `isIdentity()`:119 `screenDeltaX()`:123 `screenDeltaY()`:127 |
| [`TouchpadGeometry.java`](../../app/src/main/java/com/limelight/binding/input/TouchpadGeometry.java) | 93 | 2.7 | TouchpadGeometry | `scaleAlongX()`:62 `scaleAlongY()`:66 `isRotated()`:70 `screenDeltaX()`:79 `screenDeltaY()`:89 |
| [`GameInputDevice.java`](../../app/src/main/java/com/limelight/binding/input/GameInputDevice.java) | 19 | 6.5 | GameInputDevice | — |

## `app/src/main/java/com/limelight/binding/input/capture/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`AndroidNativePointerCaptureProvider.java`](../../app/src/main/java/com/limelight/binding/input/capture/AndroidNativePointerCaptureProvider.java) | 171 | 2.5 | AndroidNativePointerCaptureProvider | `showCursor()`:65 `hideCursor()`:76 `onWindowFocusChanged()`:89 `run()`:104 `eventHasRelativeMouseAxes()`:113 `getRelativeAxisX()`:123 `getRelativeAxisY()`:134 `onInputDeviceAdded()`:145 |
| [`ShieldCaptureProvider.java`](../../app/src/main/java/com/limelight/binding/input/capture/ShieldCaptureProvider.java) | 94 | 2.5 | ShieldCaptureProvider | `hideCursor()`:65 `showCursor()`:71 `eventHasRelativeMouseAxes()`:77 `getRelativeAxisX()`:85 `getRelativeAxisY()`:90 |
| [`InputCaptureProvider.java`](../../app/src/main/java/com/limelight/binding/input/capture/InputCaptureProvider.java) | 58 | 6.6 | InputCaptureProvider | `enableCapture()`:9 `disableCapture()`:13 `destroy()`:18 `isCapturingEnabled()`:20 `isCapturingActive()`:24 `showCursor()`:28 `hideCursor()`:32 `eventHasRelativeMouseAxes()`:36 |
| [`InputCaptureManager.java`](../../app/src/main/java/com/limelight/binding/input/capture/InputCaptureManager.java) | 39 | 2.7 | InputCaptureManager | — |
| [`AndroidPointerIconCaptureProvider.java`](../../app/src/main/java/com/limelight/binding/input/capture/AndroidPointerIconCaptureProvider.java) | 36 | 2.5 | AndroidPointerIconCaptureProvider | `hideCursor()`:25 `showCursor()`:31 |
| [`NullCaptureProvider.java`](../../app/src/main/java/com/limelight/binding/input/capture/NullCaptureProvider.java) | 5 | 2.5 | NullCaptureProvider | — |

## `app/src/main/java/com/limelight/binding/input/driver/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`ProConController.java`](../../app/src/main/java/com/limelight/binding/input/driver/ProConController.java) | 486 | 2.5 | ProConController | `start()`:212 `stop()`:242 `rumble()`:263 `rumbleTriggers()`:291 `handleRead()`:295 |
| [`UsbDriverService.java`](../../app/src/main/java/com/limelight/binding/input/driver/UsbDriverService.java) | 368 | 3.0 | UsbDriverService, UsbEventReceiver, UsbDriverBinder, UsbDriverStateListener | `reportControllerState()`:47 `reportControllerMotion()`:56 `deviceRemoved()`:64 `deviceAdded()`:75 `onReceive()`:84 `run()`:100 `setListener()`:124 `setStateListener()`:135 |
| [`XboxOneController.java`](../../app/src/main/java/com/limelight/binding/input/driver/XboxOneController.java) | 240 | 2.5 | XboxOneController, InitPacket | `handleRead()`:114 `doInit()`:171 `rumble()`:215 `rumbleTriggers()`:222 |
| [`AbstractXboxController.java`](../../app/src/main/java/com/limelight/binding/input/driver/AbstractXboxController.java) | 184 | 2.5 | AbstractXboxController | `run()`:42 `start()`:99 `stop()`:148 |
| [`Xbox360Controller.java`](../../app/src/main/java/com/limelight/binding/input/driver/Xbox360Controller.java) | 168 | 2.5 | Xbox360Controller | `handleRead()`:75 `doInit()`:142 `rumble()`:151 `rumbleTriggers()`:164 |
| [`Xbox360WirelessDongle.java`](../../app/src/main/java/com/limelight/binding/input/driver/Xbox360WirelessDongle.java) | 149 | 2.5 | Xbox360WirelessDongle | `start()`:89 `stop()`:135 `rumble()`:140 `rumbleTriggers()`:145 |
| [`AbstractController.java`](../../app/src/main/java/com/limelight/binding/input/driver/AbstractController.java) | 88 | 2.8 | AbstractController | `getControllerId()`:22 `getVendorId()`:26 `getProductId()`:30 `getSupportedButtonFlags()`:34 `getCapabilities()`:38 `getType()`:42 `setButtonFlag()`:46 `reportInput()`:54 |
| [`UsbDriverListener.java`](../../app/src/main/java/com/limelight/binding/input/driver/UsbDriverListener.java) | 13 | 2.8 | UsbDriverListener | — |

## `app/src/main/java/com/limelight/binding/input/evdev/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`EvdevCaptureProviderShim.java`](../../app/src/main/java/com/limelight/binding/input/evdev/EvdevCaptureProviderShim.java) | 25 | 3.3 | EvdevCaptureProviderShim | — |
| [`EvdevListener.java`](../../app/src/main/java/com/limelight/binding/input/evdev/EvdevListener.java) | 16 | 3.5 | EvdevListener | — |

## `app/src/main/java/com/limelight/binding/input/touch/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`TrackpadContext.java`](../../app/src/main/java/com/limelight/binding/input/touch/TrackpadContext.java) | 498 | 2.7 | TrackpadContext | `run()`:70 `run()`:77 `run()`:113 `getActionIndex()`:147 `touchDownEvent()`:182 `touchUpEvent()`:238 `touchMoveEvent()`:305 `cancelTouch()`:415 |
| [`RelativeTouchContext.java`](../../app/src/main/java/com/limelight/binding/input/touch/RelativeTouchContext.java) | 332 | 2.7 | RelativeTouchContext | `run()`:36 `run()`:57 `run()`:63 `run()`:69 `run()`:75 `run()`:81 `cancelTouch()`:306 `isCancelled()`:319 |
| [`AbsoluteTouchContext.java`](../../app/src/main/java/com/limelight/binding/input/touch/AbsoluteTouchContext.java) | 262 | 2.7 | AbsoluteTouchContext | `run()`:28 `run()`:43 `run()`:56 `cancelTouch()`:234 `isCancelled()`:251 `setPointerCount()`:256 |
| [`TouchContext.java`](../../app/src/main/java/com/limelight/binding/input/touch/TouchContext.java) | 12 | 2.7 | TouchContext | — |

## `app/src/main/java/com/limelight/binding/input/virtual_controller/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`AnalogStickFree.java`](../../app/src/main/java/com/limelight/binding/input/virtual_controller/AnalogStickFree.java) | 513 | 2.5 | AnalogStickFree, AnalogStickListener, STICK_STATE, CLICK_STATE | `addAnalogStickListener()`:189 `onSizeChanged()`:226 `onElementDraw()`:237 `setBgOpacity()`:312 `setOpacity()`:319 `onElementTouchEvent()`:351 |
| [`VirtualControllerConfigurationLoader.java`](../../app/src/main/java/com/limelight/binding/input/virtual_controller/VirtualControllerConfigurationLoader.java) | 450 | 2.5 | VirtualControllerConfigurationLoader | `onDirectionChange()`:40 `onClick()`:92 `onLongClick()`:101 `onRelease()`:110 |
| [`VirtualControllerElement.java`](../../app/src/main/java/com/limelight/binding/input/virtual_controller/VirtualControllerElement.java) | 361 | 3.3 | VirtualControllerElement, Mode | `moveElement()`:78 `resizeElement()`:92 `actionDisableEnableButton()`:104 `onDraw()`:109 `actionEnableMove()`:157 `actionEnableResize()`:161 `actionCancel()`:165 `getDefaultColor()`:170 |
| [`AnalogStick.java`](../../app/src/main/java/com/limelight/binding/input/virtual_controller/AnalogStick.java) | 350 | 2.5 | AnalogStick, AnalogStickListener, STICK_STATE, CLICK_STATE | `addAnalogStickListener()`:174 `onSizeChanged()`:211 `onElementDraw()`:221 `onElementTouchEvent()`:284 |
| [`DigitalPad.java`](../../app/src/main/java/com/limelight/binding/input/virtual_controller/DigitalPad.java) | 317 | 2.5 | DigitalPad, DigitalPadListener | `addDigitalPadListener()`:43 `onElementDraw()`:48 `rotateDrawable()`:250 `onElementTouchEvent()`:274 |
| [`VirtualController.java`](../../app/src/main/java/com/limelight/binding/input/virtual_controller/VirtualController.java) | 279 | 6.7 | VirtualController, ControllerInputContext, ControllerMode | `run()`:56 `onClick()`:93 `hide()`:129 `show()`:137 `switchShowHide()`:143 `showElements()`:153 `showEnabledElements()`:159 `removeElements()`:165 |
| [`DigitalButton.java`](../../app/src/main/java/com/limelight/binding/input/virtual_controller/DigitalButton.java) | 268 | 2.5 | DigitalButton, DigitalButtonListener | `run()`:54 `checkMovement()`:70 `addDigitalButtonListener()`:126 `setText()`:130 `setIcon()`:135 `setIconPress()`:140 `onElementDraw()`:145 `onElementTouchEvent()`:230 |
| [`LeftAnalogStickFree.java`](../../app/src/main/java/com/limelight/binding/input/virtual_controller/LeftAnalogStickFree.java) | 52 | 2.5 | LeftAnalogStickFree | `onMovement()`:19 `onClick()`:29 `onDoubleClick()`:33 `onRevoke()`:42 |
| [`RightAnalogStickFree.java`](../../app/src/main/java/com/limelight/binding/input/virtual_controller/RightAnalogStickFree.java) | 52 | 2.5 | RightAnalogStickFree | `onMovement()`:19 `onClick()`:29 `onDoubleClick()`:33 `onRevoke()`:42 |
| [`LeftAnalogStick.java`](../../app/src/main/java/com/limelight/binding/input/virtual_controller/LeftAnalogStick.java) | 50 | 2.5 | LeftAnalogStick | `onMovement()`:17 `onClick()`:27 `onDoubleClick()`:31 `onRevoke()`:40 |
| [`RightAnalogStick.java`](../../app/src/main/java/com/limelight/binding/input/virtual_controller/RightAnalogStick.java) | 50 | 2.5 | RightAnalogStick | `onMovement()`:17 `onClick()`:27 `onDoubleClick()`:31 `onRevoke()`:40 |
| [`LeftTrigger.java`](../../app/src/main/java/com/limelight/binding/input/virtual_controller/LeftTrigger.java) | 37 | 2.5 | LeftTrigger | `onClick()`:14 `onLongClick()`:23 `onRelease()`:27 |
| [`RightTrigger.java`](../../app/src/main/java/com/limelight/binding/input/virtual_controller/RightTrigger.java) | 37 | 2.5 | RightTrigger | `onClick()`:14 `onLongClick()`:23 `onRelease()`:27 |

## `app/src/main/java/com/limelight/binding/input/virtual_controller/keyboard/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`KeyBoardController.java`](../../app/src/main/java/com/limelight/binding/input/virtual_controller/keyboard/KeyBoardController.java) | 811 | 2.7 | KeyBoardController, ControllerMode | `onLongClick()`:98 `onTouch()`:113 `onClick()`:160 `hide()`:231 `hide()`:256 `show()`:260 `showElements()`:266 `showEnabledElements()`:277 |
| [`KeyBoardControllerConfigurationLoader.java`](../../app/src/main/java/com/limelight/binding/input/virtual_controller/keyboard/KeyBoardControllerConfigurationLoader.java) | 592 | 2.8 | KeyBoardControllerConfigurationLoader | `onDirectionChange()`:73 `onkeyEvent()`:121 `onkeyEvent()`:137 `onClick()`:169 `onLongClick()`:180 `onRelease()`:186 `onClick()`:198 `onLongClick()`:205 |
| [`keyBoardVirtualControllerElement.java`](../../app/src/main/java/com/limelight/binding/input/virtual_controller/keyboard/keyBoardVirtualControllerElement.java) | 439 | 2.5 | keyBoardVirtualControllerElement, Mode | `moveElement()`:83 `resizeElement()`:126 `checkAndApplyResize()`:138 `onDraw()`:165 `actionEnableMove()`:213 `actionEnableResize()`:217 `actionCancel()`:221 `getDefaultColor()`:226 |
| [`keyAnalogStickFree.java`](../../app/src/main/java/com/limelight/binding/input/virtual_controller/keyboard/keyAnalogStickFree.java) | 420 | 2.5 | keyAnalogStickFree, AnalogStickListener, STICK_STATE, CLICK_STATE | `addAnalogStickListener()`:187 `onSizeChanged()`:224 `onElementDraw()`:235 `onElementTouchEvent()`:328 |
| [`KeyBoardLayoutController.java`](../../app/src/main/java/com/limelight/binding/input/virtual_controller/keyboard/KeyBoardLayoutController.java) | 393 | 3.5 | KeyBoardLayoutController, ViewCallbacks | `isModifierKeyPressed()`:86 `onClick()`:115 `setViewCallbacks()`:125 `getHandler()`:129 `isKeyboardVisible()`:292 `hide()`:296 `hide()`:314 `show()`:318 |
| [`KeyAnalogStick.java`](../../app/src/main/java/com/limelight/binding/input/virtual_controller/keyboard/KeyAnalogStick.java) | 352 | 2.5 | KeyAnalogStick, AnalogStickListener, STICK_STATE, CLICK_STATE | `addAnalogStickListener()`:176 `onSizeChanged()`:213 `onElementDraw()`:223 `onElementTouchEvent()`:286 |
| [`KeyBoardDigitalButton.java`](../../app/src/main/java/com/limelight/binding/input/virtual_controller/keyboard/KeyBoardDigitalButton.java) | 288 | 2.5 | KeyBoardDigitalButton, DigitalButtonListener | `run()`:54 `checkMovement()`:71 `addDigitalButtonListener()`:127 `setText()`:131 `setIcon()`:136 `setSticky()`:141 `isSticky()`:145 `releaseIfSticky()`:158 |
| [`KeyBoardTouchPadButton.java`](../../app/src/main/java/com/limelight/binding/input/virtual_controller/keyboard/KeyBoardTouchPadButton.java) | 282 | 2.5 | KeyBoardTouchPadButton, DigitalButtonListener | `run()`:56 `checkMovement()`:72 `addDigitalButtonListener()`:129 `setText()`:133 `setIcon()`:138 `onElementDraw()`:148 `onElementTouchEvent()`:222 |
| [`KeyboardDigitalPadButton.java`](../../app/src/main/java/com/limelight/binding/input/virtual_controller/keyboard/KeyboardDigitalPadButton.java) | 203 | 2.5 | KeyboardDigitalPadButton, DigitalPadListener | `addDigitalPadListener()`:32 `onElementDraw()`:37 `onElementTouchEvent()`:160 |
| [`LayoutSnappingHelper.java`](../../app/src/main/java/com/limelight/binding/input/virtual_controller/keyboard/LayoutSnappingHelper.java) | 158 | 2.5 | LayoutSnappingHelper, SnapResult | — |
| [`KeyBoardAnalogStickButton.java`](../../app/src/main/java/com/limelight/binding/input/virtual_controller/keyboard/KeyBoardAnalogStickButton.java) | 155 | 2.5 | KeyBoardAnalogStickButton, KeyBoardAnalogStickListener | `setListener()`:21 `onMovement()`:34 `onClick()`:124 `onDoubleClick()`:129 `onRevoke()`:134 |
| [`KeyBoardAnalogStickButtonFree.java`](../../app/src/main/java/com/limelight/binding/input/virtual_controller/keyboard/KeyBoardAnalogStickButtonFree.java) | 155 | 2.5 | KeyBoardAnalogStickButtonFree, KeyBoardAnalogStickListener | `setListener()`:21 `onMovement()`:34 `onClick()`:124 `onDoubleClick()`:129 `onRevoke()`:134 |

## `app/src/main/java/com/limelight/binding/video/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`MediaCodecDecoderRenderer.java`](../../app/src/main/java/com/limelight/binding/video/MediaCodecDecoderRenderer.java) | 2485 | 2.7 | MediaCodecDecoderRenderer, DecoderHungException, RendererException | `setForceTightThresholds()`:55 `setPreferLowerDelaysTimeoutUs()`:63 `setPreferLowerDelays()`:100 `setRenderTarget()`:361 `isHevcSupported()`:456 `isAvcSupported()`:460 `isHevcMain10Hdr10Supported()`:464 `isAv1Supported()`:479 |
| [`MediaCodecHelper.java`](../../app/src/main/java/com/limelight/binding/video/MediaCodecHelper.java) | 1206 | 3.0 | MediaCodecHelper | — |
| [`VideoStats.java`](../../app/src/main/java/com/limelight/binding/video/VideoStats.java) | 93 | 2.5 | VideoStats, VideoStatsFps | — |
| [`FramePacingRoute.java`](../../app/src/main/java/com/limelight/binding/video/FramePacingRoute.java) | 48 | 2.5 | FramePacingRoute | — |
| [`CrashListener.java`](../../app/src/main/java/com/limelight/binding/video/CrashListener.java) | 6 | 2.7 | CrashListener | — |
| [`PerfOverlayListener.java`](../../app/src/main/java/com/limelight/binding/video/PerfOverlayListener.java) | 6 | 2.7 | PerfOverlayListener | — |

## `app/src/main/java/com/limelight/computers/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`ComputerManagerService.java`](../../app/src/main/java/com/limelight/computers/ComputerManagerService.java) | 997 | 5.0 | ComputerManagerService, ComputerManagerBinder, ParallelPollTuple, ApplistPoller, PollingTuple, ReachabilityTuple | `onServiceConnected()`:73 `onServiceDisconnected()`:86 `run()`:169 `startPolling()`:199 `waitForReady()`:229 `waitForPollingStopped()`:247 `addComputerBlocking()`:262 `removeComputer()`:266 |
| [`ComputerDatabaseManager.java`](../../app/src/main/java/com/limelight/computers/ComputerDatabaseManager.java) | 236 | 2.7 | ComputerDatabaseManager, AddressFields | `close()`:58 `deleteComputer()`:84 `updateComputer()`:110 `getAllComputers()`:183 `getComputerByName()`:202 `getComputerByUUID()`:222 |
| [`LegacyDatabaseReader3.java`](../../app/src/main/java/com/limelight/computers/LegacyDatabaseReader3.java) | 124 | 2.5 | LegacyDatabaseReader3 | — |
| [`LegacyDatabaseReader.java`](../../app/src/main/java/com/limelight/computers/LegacyDatabaseReader.java) | 103 | 2.5 | LegacyDatabaseReader | — |
| [`LegacyDatabaseReader2.java`](../../app/src/main/java/com/limelight/computers/LegacyDatabaseReader2.java) | 84 | 2.5 | LegacyDatabaseReader2 | — |
| [`IdentityManager.java`](../../app/src/main/java/com/limelight/computers/IdentityManager.java) | 74 | 2.5 | IdentityManager | `getUniqueId()`:29 |
| [`ComputerManagerListener.java`](../../app/src/main/java/com/limelight/computers/ComputerManagerListener.java) | 8 | 3.0 | ComputerManagerListener | — |

## `app/src/main/java/com/limelight/discovery/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`DiscoveryService.java`](../../app/src/main/java/com/limelight/discovery/DiscoveryService.java) | 91 | 2.9 | DiscoveryService, DiscoveryBinder | `setListener()`:23 `startDiscovery()`:27 `stopDiscovery()`:31 `getComputerSet()`:35 `onCreate()`:41 `notifyComputerAdded()`:44 `notifyDiscoveryFailure()`:51 `onBind()`:77 |

## `app/src/main/java/com/limelight/grid/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`AppGridAdapter.java`](../../app/src/main/java/com/limelight/grid/AppGridAdapter.java) | 352 | 2.8 | AppGridAdapter | `getOutline()`:59 `updateHiddenApps()`:91 `updateLayoutWithPreferences()`:125 `cancelQueuedOperations()`:159 `compare()`:168 `addApp()`:180 `removeApp()`:199 `clear()`:205 |
| [`GenericGridAdapter.java`](../../app/src/main/java/com/limelight/grid/GenericGridAdapter.java) | 77 | 2.5 | GenericGridAdapter | `clear()`:39 `getCount()`:44 `getItem()`:49 `getItemId()`:54 `getView()`:61 |
| [`AppObject.java`](../../app/src/main/java/com/limelight/grid/AppObject.java) | 33 | 2.8 | AppObject | `toString()`:29 |

## `app/src/main/java/com/limelight/grid/assets/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`CachedAppAssetLoader.java`](../../app/src/main/java/com/limelight/grid/assets/CachedAppAssetLoader.java) | 496 | 2.8 | CachedAppAssetLoader, ArtAspectListener, LoaderTask, AsyncDrawable, LoaderTuple | `cancelBackgroundLoads()`:94 `cancelForegroundLoads()`:101 `setArtAspectListener()`:113 `freeCacheMemory()`:129 `doInBackground()`:197 `onProgressUpdate()`:226 `onPostExecute()`:249 `onAnimationStart()`:274 |
| [`DiskAssetLoader.java`](../../app/src/main/java/com/limelight/grid/assets/DiskAssetLoader.java) | 186 | 5.5 | DiskAssetLoader | `checkCacheExists()`:35 `loadBitmapFromCache()`:61 `onHeaderDecoded()`:116 `getFile()`:146 `getBoxArtDirectory()`:155 `deleteAssetsForComputer()`:159 `populateCacheWithStream()`:169 |
| [`MemoryAssetLoader.java`](../../app/src/main/java/com/limelight/grid/assets/MemoryAssetLoader.java) | 75 | 2.8 | MemoryAssetLoader | `sizeOf()`:14 `entryRemoved()`:20 `loadBitmapFromCache()`:35 `populateCache()`:65 `clearCache()`:69 |
| [`NetworkAssetLoader.java`](../../app/src/main/java/com/limelight/grid/assets/NetworkAssetLoader.java) | 41 | 2.8 | NetworkAssetLoader | `getBitmapStream()`:22 |
| [`ScaledBitmap.java`](../../app/src/main/java/com/limelight/grid/assets/ScaledBitmap.java) | 19 | 2.5 | ScaledBitmap | — |

## `app/src/main/java/com/limelight/nvstream/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`NvConnection.java`](../../app/src/main/java/com/limelight/nvstream/NvConnection.java) | 678 | 6.6 | NvConnection | `hasStartedNativeConnection()`:110 `stop()`:114 `quitAndLaunch()`:374 `run()`:425 `sendExecServerCmd()`:533 `sendKeyboardInput()`:586 `sendMouseScroll()`:592 `sendMouseHScroll()`:598 |
| [`StreamConfiguration.java`](../../app/src/main/java/com/limelight/nvstream/StreamConfiguration.java) | 262 | 2.7 | StreamConfiguration, Builder | `setApp()`:38 `setRemoteConfiguration()`:43 `setResolution()`:48 `setRefreshRate()`:54 `setLaunchRefreshRate()`:59 `setVirtualDisplay()`:64 `setResolutionScaleFactor()`:69 `setBitrate()`:74 |
| [`ConnectionContext.java`](../../app/src/main/java/com/limelight/nvstream/ConnectionContext.java) | 35 | 4.6 | ConnectionContext | — |
| [`NvConnectionListener.java`](../../app/src/main/java/com/limelight/nvstream/NvConnectionListener.java) | 24 | 16.7 | NvConnectionListener | — |

## `app/src/main/java/com/limelight/nvstream/av/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`ByteBufferDescriptor.java`](../../app/src/main/java/com/limelight/nvstream/av/ByteBufferDescriptor.java) | 58 | 2.5 | ByteBufferDescriptor | — |

## `app/src/main/java/com/limelight/nvstream/av/audio/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`AudioRenderer.java`](../../app/src/main/java/com/limelight/nvstream/av/audio/AudioRenderer.java) | 16 | 18.9 | AudioRenderer | — |

## `app/src/main/java/com/limelight/nvstream/av/video/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`VideoDecoderRenderer.java`](../../app/src/main/java/com/limelight/nvstream/av/video/VideoDecoderRenderer.java) | 22 | 17.5 | VideoDecoderRenderer | — |

## `app/src/main/java/com/limelight/nvstream/http/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`NvHTTP.java`](../../app/src/main/java/com/limelight/nvstream/http/NvHTTP.java) | 939 | 9.6 | NvHTTP | `chooseClientAlias()`:116 `chooseServerAlias()`:118 `getCertificateChain()`:120 `getClientAliases()`:123 `getPrivateKey()`:124 `getServerAliases()`:127 `getAcceptedIssuers()`:132 `checkClientTrusted()`:135 |
| [`PairingManager.java`](../../app/src/main/java/com/limelight/nvstream/http/PairingManager.java) | 357 | 5.7 | PairingManager, PairState, PairingHashAlgorithm, Sha1PairingHash, Sha256PairingHash | `getPairedCert()`:183 `pair()`:187 `getHashLength()`:324 `hashData()`:328 `getHashLength()`:341 `hashData()`:345 |
| [`ComputerDetails.java`](../../app/src/main/java/com/limelight/nvstream/http/ComputerDetails.java) | 244 | 22.5 | ComputerDetails, State, AddressTuple | `hashCode()`:37 `equals()`:42 `toString()`:51 `guessExternalPort()`:102 `update()`:123 `getCapabilities()`:179 `toString()`:185 |
| [`HostCapabilities.java`](../../app/src/main/java/com/limelight/nvstream/http/HostCapabilities.java) | 159 | 3.9 | HostCapabilities | `getPermissionMask()`:129 `hasPermissionModel()`:134 `supportsClipboardSend()`:143 `supportsClipboardFetch()`:148 |
| [`NvApp.java`](../../app/src/main/java/com/limelight/nvstream/http/NvApp.java) | 101 | 10.5 | NvApp | `setAppName()`:28 `setAppUUID()`:32 `setAppId()`:36 `setAppIndex()`:45 `setAppId()`:54 `setAppIndex()`:59 `setHdrSupported()`:63 `getAppName()`:67 |
| [`HostHttpResponseException.java`](../../app/src/main/java/com/limelight/nvstream/http/HostHttpResponseException.java) | 29 | 3.3 | HostHttpResponseException | `getErrorCode()`:16 `getErrorMessage()`:20 `getMessage()`:25 |
| [`LimelightCryptoProvider.java`](../../app/src/main/java/com/limelight/nvstream/http/LimelightCryptoProvider.java) | 12 | 5.4 | LimelightCryptoProvider | — |

## `app/src/main/java/com/limelight/nvstream/input/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`ControllerPacket.java`](../../app/src/main/java/com/limelight/nvstream/input/ControllerPacket.java) | 27 | 15.8 | ControllerPacket | — |
| [`MouseButtonPacket.java`](../../app/src/main/java/com/limelight/nvstream/input/MouseButtonPacket.java) | 13 | 6.2 | MouseButtonPacket | — |
| [`KeyboardPacket.java`](../../app/src/main/java/com/limelight/nvstream/input/KeyboardPacket.java) | 11 | 6.0 | KeyboardPacket | — |

## `app/src/main/java/com/limelight/nvstream/jni/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`MoonBridge.java`](../../app/src/main/java/com/limelight/nvstream/jni/MoonBridge.java) | 445 | 49.5 | MoonBridge, AudioConfiguration | `getSurroundAudioInfo()`:167 `equals()`:172 `hashCode()`:182 `toInt()`:188 `startConnection()`:345 `stopConnection()`:355 `interruptConnection()`:357 `sendExecServerCmd()`:359 |

## `app/src/main/java/com/limelight/nvstream/mdns/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`JmDNSDiscoveryAgent.java`](../../app/src/main/java/com/limelight/nvstream/mdns/JmDNSDiscoveryAgent.java) | 270 | 3.0 | JmDNSDiscoveryAgent, MyNetworkTopologyDiscovery | `serviceAdded()`:34 `serviceRemoved()`:50 `serviceResolved()`:66 `useInetAddress()`:84 `newNetworkTopologyDiscovery()`:113 `startDiscovery()`:169 `run()`:183 `stopDiscovery()`:227 |
| [`NsdManagerDiscoveryAgent.java`](../../app/src/main/java/com/limelight/nvstream/mdns/NsdManagerDiscoveryAgent.java) | 235 | 3.0 | NsdManagerDiscoveryAgent | `onStartDiscoveryFailed()`:33 `onStopDiscoveryFailed()`:49 `onDiscoveryStarted()`:63 `onDiscoveryStopped()`:79 `onServiceFound()`:92 `onServiceInfoCallbackRegistrationFailed()`:104 `onServiceUpdated()`:110 `onServiceLost()`:118 |
| [`MdnsDiscoveryAgent.java`](../../app/src/main/java/com/limelight/nvstream/mdns/MdnsDiscoveryAgent.java) | 149 | 3.0 | MdnsDiscoveryAgent | `reportNewComputer()`:24 `getComputerSet()`:55 |
| [`MdnsComputer.java`](../../app/src/main/java/com/limelight/nvstream/mdns/MdnsComputer.java) | 72 | 3.4 | MdnsComputer | `getName()`:19 `getLocalAddress()`:23 `getIpv6Address()`:27 `getPort()`:31 `hashCode()`:36 `equals()`:41 `toString()`:68 |
| [`MdnsDiscoveryListener.java`](../../app/src/main/java/com/limelight/nvstream/mdns/MdnsDiscoveryListener.java) | 7 | 3.4 | MdnsDiscoveryListener | — |

## `app/src/main/java/com/limelight/nvstream/wol/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`WakeOnLanSender.java`](../../app/src/main/java/com/limelight/nvstream/wol/WakeOnLanSender.java) | 150 | 2.8 | WakeOnLanSender | — |

## `app/src/main/java/com/limelight/preferences/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`StreamSettings.java`](../../app/src/main/java/com/limelight/preferences/StreamSettings.java) | 1490 | 3.2 | StreamSettings, SettingsFragment | `onCreate()`:163 `onAttachedToWindow()`:265 `onConfigurationChanged()`:283 `onBackPressed()`:302 `getOpenScreenKey()`:371 `setInitialScreenKey()`:376 `backToIndex()`:381 `onNavigateToScreen()`:390 |
| [`PreferenceConfiguration.java`](../../app/src/main/java/com/limelight/preferences/PreferenceConfiguration.java) | 1147 | 47.5 | PreferenceConfiguration, ScaleMode, ImeDisplayMode, FormatOption, AnalogStickForScrolling | — |
| [`AddComputerManually.java`](../../app/src/main/java/com/limelight/preferences/AddComputerManually.java) | 597 | 4.1 | AddComputerManually | `onServiceConnected()`:64 `onServiceDisconnected()`:69 `run()`:247 `run()`:331 `run()`:362 `onStop()`:416 `onDestroy()`:424 `onCreate()`:495 |
| [`SeekBarPreference.java`](../../app/src/main/java/com/limelight/preferences/SeekBarPreference.java) | 420 | 2.5 | SeekBarPreference | `onBindViewHolder()`:145 `onClick()`:197 `getDialog()`:214 `onProgressChanged()`:285 `onStartTrackingTouch()`:303 `onStopTrackingTouch()`:306 `updateSeekbar()`:361 `setProgress()`:398 |
| [`TouchpadCalibrationActivity.java`](../../app/src/main/java/com/limelight/preferences/TouchpadCalibrationActivity.java) | 190 | 2.7 | TouchpadCalibrationActivity | `onCreate()`:49 `onClick()`:59 `onGenericMotionEvent()`:96 `onTouchEvent()`:126 `run()`:184 |
| [`LanguagePreference.java`](../../app/src/main/java/com/limelight/preferences/LanguagePreference.java) | 62 | 2.5 | LanguagePreference | `onClick()`:45 |
| [`ConfirmDeleteKeyboardPreference.java`](../../app/src/main/java/com/limelight/preferences/ConfirmDeleteKeyboardPreference.java) | 57 | 2.5 | ConfirmDeleteKeyboardPreference, DialogFragmentCompat | `onDialogClosed()`:48 |
| [`ConfirmDeleteOscPreference.java`](../../app/src/main/java/com/limelight/preferences/ConfirmDeleteOscPreference.java) | 53 | 2.5 | ConfirmDeleteOscPreference, DialogFragmentCompat | `onDialogClosed()`:45 |
| [`WebLauncherPreference.java`](../../app/src/main/java/com/limelight/preferences/WebLauncherPreference.java) | 46 | 2.5 | WebLauncherPreference | `onClick()`:42 |
| [`GlPreferences.java`](../../app/src/main/java/com/limelight/preferences/GlPreferences.java) | 38 | 2.8 | GlPreferences | `writePreferences()`:31 |
| [`SmallIconCheckboxPreference.java`](../../app/src/main/java/com/limelight/preferences/SmallIconCheckboxPreference.java) | 33 | 2.5 | SmallIconCheckboxPreference | `onGetDefaultValue()`:29 |

## `app/src/main/java/com/limelight/profiles/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`ProfilesManager.java`](../../app/src/main/java/com/limelight/profiles/ProfilesManager.java) | 266 | 28.0 | ProfilesManager, ProfilesData, ProfileChangeListener, OverlaySharedPreferences | `load()`:49 `save()`:102 `getProfiles()`:133 `add()`:137 `update()`:143 `delete()`:149 `setActive()`:158 `getActive()`:164 |
| [`ProfilesAdapter.java`](../../app/src/main/java/com/limelight/profiles/ProfilesAdapter.java) | 117 | 3.5 | ProfilesAdapter, ProfileViewHolder | `onCreateViewHolder()`:36 `onBindViewHolder()`:42 `getItemCount()`:97 |
| [`SettingsProfile.java`](../../app/src/main/java/com/limelight/profiles/SettingsProfile.java) | 62 | 4.0 | SettingsProfile | `getUuid()`:23 `getName()`:27 `setName()`:31 `getCreatedUtc()`:35 `getModifiedUtc()`:39 `setModifiedUtc()`:43 `getOptions()`:47 `setOptions()`:51 |

## `app/src/main/java/com/limelight/ui/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`GavetaDaSessao.java`](../../app/src/main/java/com/limelight/ui/GavetaDaSessao.java) | 738 | 2.7 | GavetaDaSessao, Ouvinte | `ligar()`:176 `aplicar()`:182 `run()`:192 `onLayout()`:199 `onFinishInflate()`:254 `onInterceptTouchEvent()`:395 `onTouchEvent()`:491 `aplicar()`:576 |
| [`VidroSession.java`](../../app/src/main/java/com/limelight/ui/VidroSession.java) | 708 | 2.7 | VidroSession, Controls | `showLost()`:144 `onClick()`:173 `onClick()`:185 `isLostShowing()`:200 `showConnecting()`:256 `onStageStarting()`:294 `onConnected()`:326 `run()`:371 |
| [`VidroLightView.java`](../../app/src/main/java/com/limelight/ui/VidroLightView.java) | 387 | 2.8 | VidroLightView, FloatSink | `setTemperatura()`:189 `setIntensity()`:198 `set()`:229 `setQuality()`:236 `set()`:259 `set()`:265 `onAnimationUpdate()`:282 `onDetachedFromWindow()`:292 |
| [`StreamContainer.java`](../../app/src/main/java/com/limelight/ui/StreamContainer.java) | 316 | 2.7 | StreamContainer, InputCallbacks, StreamMode | `init()`:66 `setDesiredAspectRatio()`:104 `setFillDisplay()`:109 `onMeasure()`:115 `setInputCallbacks()`:194 `setCommitTextEnabled()`:198 `onKeyPreIme()`:203 `onWindowFocusChanged()`:215 |
| [`PaletaDeComandos.java`](../../app/src/main/java/com/limelight/ui/PaletaDeComandos.java) | 280 | 2.7 | PaletaDeComandos, Comando | `aplicar()`:90 `beforeTextChanged()`:97 `onTextChanged()`:101 `afterTextChanged()`:105 `definirComandos()`:111 `recolher()`:118 `estaNaLista()`:125 `aoTeclar()`:138 |
| [`Mola.java`](../../app/src/main/java/com/limelight/ui/Mola.java) | 245 | 2.7 | Mola, Alvo, AoChegar, Perfil | `lancar()`:134 `lancar()`:147 `fixar()`:157 `cancelar()`:165 `valor()`:173 `velocidade()`:177 `correndo()`:181 `doFrame()`:186 |
| [`MoqueDaStream.java`](../../app/src/main/java/com/limelight/ui/MoqueDaStream.java) | 181 | 2.7 | MoqueDaStream | `comecar()`:60 `parar()`:72 `run()`:80 |
| [`StreamView.java`](../../app/src/main/java/com/limelight/ui/StreamView.java) | 168 | 2.5 | StreamView, InputCallbacks | `setDesiredAspectRatio()`:24 `setInputCallbacks()`:28 `setFillDisplay()`:32 `setCommitTextEnabled()`:36 `onMeasure()`:62 `onKeyPreIme()`:97 `onWindowFocusChanged()`:117 `onCheckIsTextEditor()`:126 |
| [`MolduraDeEstado.java`](../../app/src/main/java/com/limelight/ui/MolduraDeEstado.java) | 167 | 2.7 | MolduraDeEstado | `setEstado()`:116 `onAnimationUpdate()`:134 `onDetachedFromWindow()`:147 `onDraw()`:156 |
| [`ApertureViewGroup.java`](../../app/src/main/java/com/limelight/ui/ApertureViewGroup.java) | 151 | 2.5 | ApertureViewGroup | `getOutline()`:54 `getCurrentSpeed()`:80 `setCurrentSpeed()`:84 `onSizeChanged()`:90 `dispatchDraw()`:124 |
| [`VidroPosterBox.java`](../../app/src/main/java/com/limelight/ui/VidroPosterBox.java) | 99 | 2.8 | VidroPosterBox | `setArtAspect()`:61 `onMeasure()`:83 |
| [`ExternalControllerView.java`](../../app/src/main/java/com/limelight/ui/ExternalControllerView.java) | 94 | 3.5 | ExternalControllerView, InputCallbacks | `setInputCallbacks()`:16 `setCommitTextEnabled()`:20 `onKeyPreIme()`:34 `onCheckIsTextEditor()`:54 `onCreateInputConnection()`:59 `commitText()`:70 `deleteSurroundingText()`:78 |
| [`PrazoDaSessao.java`](../../app/src/main/java/com/limelight/ui/PrazoDaSessao.java) | 91 | 2.7 | PrazoDaSessao | `armado()`:56 `armar()`:61 `desarmar()`:70 `aoMudarPiP()`:84 |
| [`VidroToast.java`](../../app/src/main/java/com/limelight/ui/VidroToast.java) | 85 | 21.2 | VidroToast | — |
| [`AdapterFragment.java`](../../app/src/main/java/com/limelight/ui/AdapterFragment.java) | 36 | 2.5 | AdapterFragment | `onAttach()`:18 `onCreateView()`:25 `onActivityCreated()`:31 |
| [`GameGestures.java`](../../app/src/main/java/com/limelight/ui/GameGestures.java) | 10 | 3.0 | GameGestures | — |
| [`AdapterFragmentCallbacks.java`](../../app/src/main/java/com/limelight/ui/AdapterFragmentCallbacks.java) | 9 | 2.5 | AdapterFragmentCallbacks | — |

## `app/src/main/java/com/limelight/ui/compose/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`ArvoreDeAjustes.kt`](../../app/src/main/java/com/limelight/ui/compose/ArvoreDeAjustes.kt) | 1190 | — | — | — |
| [`TelaAjustes.kt`](../../app/src/main/java/com/limelight/ui/compose/TelaAjustes.kt) | 1079 | — | — | — |
| [`TelaHome.kt`](../../app/src/main/java/com/limelight/ui/compose/TelaHome.kt) | 605 | — | — | — |
| [`TelaEditorDeCena.kt`](../../app/src/main/java/com/limelight/ui/compose/TelaEditorDeCena.kt) | 572 | — | — | — |
| [`GradeDeHosts.kt`](../../app/src/main/java/com/limelight/ui/compose/GradeDeHosts.kt) | 375 | — | — | — |
| [`TelaCatalogo.kt`](../../app/src/main/java/com/limelight/ui/compose/TelaCatalogo.kt) | 374 | — | class | — |
| [`TelaCenas.kt`](../../app/src/main/java/com/limelight/ui/compose/TelaCenas.kt) | 337 | — | — | — |
| [`FolhaVidro.kt`](../../app/src/main/java/com/limelight/ui/compose/FolhaVidro.kt) | 301 | — | — | — |
| [`TelaPareamento.kt`](../../app/src/main/java/com/limelight/ui/compose/TelaPareamento.kt) | 300 | — | class | — |
| [`TelaEndereco.kt`](../../app/src/main/java/com/limelight/ui/compose/TelaEndereco.kt) | 290 | — | — | — |
| [`Casca.kt`](../../app/src/main/java/com/limelight/ui/compose/Casca.kt) | 275 | — | Rota, EstadoDoCatalogo, EstadoDaCasca | — |
| [`TelaDepuracao.kt`](../../app/src/main/java/com/limelight/ui/compose/TelaDepuracao.kt) | 199 | — | — | — |
| [`PonteDaCasca.kt`](../../app/src/main/java/com/limelight/ui/compose/PonteDaCasca.kt) | 180 | — | PonteDaCasca | — |
| [`PonteDeAjustes.kt`](../../app/src/main/java/com/limelight/ui/compose/PonteDeAjustes.kt) | 173 | — | — | — |
| [`PonteDoCatalogo.kt`](../../app/src/main/java/com/limelight/ui/compose/PonteDoCatalogo.kt) | 166 | — | — | — |
| [`PonteDoEditorDeCena.kt`](../../app/src/main/java/com/limelight/ui/compose/PonteDoEditorDeCena.kt) | 162 | — | — | — |
| [`EstadoDaHome.kt`](../../app/src/main/java/com/limelight/ui/compose/EstadoDaHome.kt) | 127 | — | EstadoDaHome | — |
| [`HostUi.kt`](../../app/src/main/java/com/limelight/ui/compose/HostUi.kt) | 126 | — | class | — |
| [`AjusteUi.kt`](../../app/src/main/java/com/limelight/ui/compose/AjusteUi.kt) | 125 | — | AjusteUi | — |
| [`PeneiraDeAjustes.kt`](../../app/src/main/java/com/limelight/ui/compose/PeneiraDeAjustes.kt) | 120 | — | — | — |
| [`ResumoDosHosts.kt`](../../app/src/main/java/com/limelight/ui/compose/ResumoDosHosts.kt) | 106 | — | — | — |
| [`Marca.kt`](../../app/src/main/java/com/limelight/ui/compose/Marca.kt) | 101 | — | — | — |
| [`Historico.kt`](../../app/src/main/java/com/limelight/ui/compose/Historico.kt) | 86 | — | — | — |
| [`Compartilhamento.kt`](../../app/src/main/java/com/limelight/ui/compose/Compartilhamento.kt) | 84 | — | Compartilhamento | — |
| [`PonteDaHome.kt`](../../app/src/main/java/com/limelight/ui/compose/PonteDaHome.kt) | 82 | — | — | — |
| [`Modificadores.kt`](../../app/src/main/java/com/limelight/ui/compose/Modificadores.kt) | 76 | — | — | — |
| [`CenaEmEdicao.kt`](../../app/src/main/java/com/limelight/ui/compose/CenaEmEdicao.kt) | 72 | — | class, class | — |
| [`Molas.kt`](../../app/src/main/java/com/limelight/ui/compose/Molas.kt) | 49 | — | — | — |
| [`PonteDoPareamento.kt`](../../app/src/main/java/com/limelight/ui/compose/PonteDoPareamento.kt) | 47 | — | — | — |
| [`PonteDoEndereco.kt`](../../app/src/main/java/com/limelight/ui/compose/PonteDoEndereco.kt) | 35 | — | — | — |
| [`PonteDeCenas.kt`](../../app/src/main/java/com/limelight/ui/compose/PonteDeCenas.kt) | 31 | — | — | — |
| [`PonteDaDepuracao.kt`](../../app/src/main/java/com/limelight/ui/compose/PonteDaDepuracao.kt) | 27 | — | — | — |

## `app/src/main/java/com/limelight/utils/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`Stereo3DRenderer.java`](../../app/src/main/java/com/limelight/utils/Stereo3DRenderer.java) | 1149 | 3.5 | Stereo3DRenderer, OnSurfaceReadyListener, InferenceResult, RenderResult, AiTask, AiResultHandling | `setPrefConfig()`:156 `onSurfaceDestroyed()`:160 `getVideoSurface()`:223 `onFrameAvailable()`:228 `onSurfaceCreated()`:236 `onDrawFrame()`:414 `onSurfaceChanged()`:756 `run()`:981 |
| [`KeyMapper.java`](../../app/src/main/java/com/limelight/utils/KeyMapper.java) | 1077 | 5.1 | KeyMapper | — |
| [`ExternalDisplayControlActivity.java`](../../app/src/main/java/com/limelight/utils/ExternalDisplayControlActivity.java) | 568 | 7.7 | ExternalDisplayControlActivity | `onCreate()`:115 `onResume()`:200 `onPause()`:208 `onDestroy()`:216 `onKeyboardControllerVisibilityChange()`:222 `onWindowFocusChanged()`:280 `onBackPressed()`:290 `onConfigurationChanged()`:317 |
| [`UiHelper.java`](../../app/src/main/java/com/limelight/utils/UiHelper.java) | 419 | 10.0 | UiHelper | `onApplyWindowInsets()`:113 `onApplyWindowInsets()`:158 `onApplyWindowInsets()`:211 `run()`:261 `run()`:273 |
| [`DeviceUtils.java`](../../app/src/main/java/com/limelight/utils/DeviceUtils.java) | 411 | 6.0 | DeviceUtils | — |
| [`TvChannelHelper.java`](../../app/src/main/java/com/limelight/utils/TvChannelHelper.java) | 367 | 2.5 | TvChannelHelper, PreviewProgramBuilder, ChannelBuilder | `setChannelId()`:285 `setType()`:290 `setTitle()`:295 `setPosterArtAspectRatio()`:300 `setIntent()`:305 `setIntentUri()`:310 `setInternalProviderId()`:315 `setPosterArtUri()`:320 |
| [`ServerHelper.java`](../../app/src/main/java/com/limelight/utils/ServerHelper.java) | 354 | 4.4 | ServerHelper | `run()`:198 `run()`:261 |
| [`ShortcutHelper.java`](../../app/src/main/java/com/limelight/utils/ShortcutHelper.java) | 316 | 3.2 | ShortcutHelper | `reportComputerShortcutUsed()`:98 `reportGameLaunched()`:106 `createAppViewShortcut()`:111 `createAppViewShortcutForOnlineHost()`:156 `createPinnedGameShortcut()`:165 `disableComputerShortcut()`:187 `disableAppShortcut()`:207 `enableAppShortcut()`:217 |
| [`Dialog.java`](../../app/src/main/java/com/limelight/utils/Dialog.java) | 261 | 3.7 | Dialog | `run()`:61 `run()`:91 `run()`:112 `run()`:126 `onClick()`:159 `onClick()`:172 `onClick()`:185 `onShow()`:199 |
| [`ShaderUtils.java`](../../app/src/main/java/com/limelight/utils/ShaderUtils.java) | 202 | 2.5 | ShaderUtils | — |
| [`PanZoomHandler.java`](../../app/src/main/java/com/limelight/utils/PanZoomHandler.java) | 199 | 2.7 | PanZoomHandler, ScaleListener, GestureListener | `handleTouchEvent()`:42 `handleSurfaceChange()`:79 `applyScale()`:119 `applyTrackpadPinch()`:145 `isZoomed()`:153 `onScale()`:159 `onScaleEnd()`:165 `onScroll()`:172 |
| [`NetworkScope.java`](../../app/src/main/java/com/limelight/utils/NetworkScope.java) | 179 | 2.7 | NetworkScope | — |
| [`SpinnerDialog.java`](../../app/src/main/java/com/limelight/utils/SpinnerDialog.java) | 175 | 9.0 | SpinnerDialog | `run()`:105 `run()`:115 `onCancel()`:159 |
| [`PerformanceDataTracker.java`](../../app/src/main/java/com/limelight/utils/PerformanceDataTracker.java) | 148 | 3.0 | PerformanceDataTracker | `savePerformanceStatistics()`:34 `getLog()`:137 `clearLogs()`:142 |
| [`FileUriUtils.java`](../../app/src/main/java/com/limelight/utils/FileUriUtils.java) | 103 | 2.8 | FileUriUtils | — |
| [`CacheHelper.java`](../../app/src/main/java/com/limelight/utils/CacheHelper.java) | 87 | 5.6 | CacheHelper | — |
| [`ResumeDecision.java`](../../app/src/main/java/com/limelight/utils/ResumeDecision.java) | 85 | 2.6 | ResumeDecision, HostTap, AppTap | — |
| [`ReflectivePaddingInt8Minimal.java`](../../app/src/main/java/com/limelight/utils/ReflectivePaddingInt8Minimal.java) | 80 | 2.5 | ReflectivePaddingInt8Minimal | — |
| [`HelpLauncher.java`](../../app/src/main/java/com/limelight/utils/HelpLauncher.java) | 60 | 4.8 | HelpLauncher | — |
| [`Vector2d.java`](../../app/src/main/java/com/limelight/utils/Vector2d.java) | 48 | 2.8 | Vector2d | `initialize()`:14 `getMagnitude()`:20 `getNormalized()`:24 `scalarMultiply()`:28 `setX()`:32 `setY()`:36 `getX()`:40 `getY()`:44 |
| [`KeyConfigHelper.java`](../../app/src/main/java/com/limelight/utils/KeyConfigHelper.java) | 45 | 4.3 | KeyConfigHelper, ShortcutFile, Shortcut | — |
| [`TrafficStatsHelper.java`](../../app/src/main/java/com/limelight/utils/TrafficStatsHelper.java) | 37 | 2.9 | TrafficStatsHelper | — |
| [`NetHelper.java`](../../app/src/main/java/com/limelight/utils/NetHelper.java) | 33 | 2.9 | NetHelper | — |
| [`MouseModeOption.java`](../../app/src/main/java/com/limelight/utils/MouseModeOption.java) | 17 | 2.7 | MouseModeOption | `toString()`:13 |

## `app/src/main/jni/evdev_reader/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`evdev_reader.c`](../../app/src/main/jni/evdev_reader/evdev_reader.c) | 412 | — | — | — |

## `app/src/main/jni/moonlight-core/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`controller_list.h`](../../app/src/main/jni/moonlight-core/controller_list.h) | 593 | — | — | — |
| [`callbacks.c`](../../app/src/main/jni/moonlight-core/callbacks.c) | 520 | — | — | — |
| [`simplejni.c`](../../app/src/main/jni/moonlight-core/simplejni.c) | 288 | — | — | — |
| [`usb_ids.h`](../../app/src/main/jni/moonlight-core/usb_ids.h) | 168 | — | — | — |
| [`minisdl.c`](../../app/src/main/jni/moonlight-core/minisdl.c) | 107 | — | — | — |
| [`controller_type.h`](../../app/src/main/jni/moonlight-core/controller_type.h) | 78 | — | — | — |
| [`minisdl.h`](../../app/src/main/jni/moonlight-core/minisdl.h) | 11 | — | — | — |

## `app/src/main/jni/moonlight-core/moonlight-common-c/src/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`ControlStream.c`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/ControlStream.c) | 2077 | — | — | — |
| [`InputStream.c`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/InputStream.c) | 1603 | — | — | — |
| [`RtspConnection.c`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/RtspConnection.c) | 1420 | — | — | — |
| [`VideoDepacketizer.c`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/VideoDepacketizer.c) | 1189 | — | — | — |
| [`PlatformSockets.c`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/PlatformSockets.c) | 1001 | — | — | — |
| [`Limelight.h`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/Limelight.h) | 978 | — | — | — |
| [`RtpVideoQueue.c`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/RtpVideoQueue.c) | 802 | — | — | — |
| [`RtpAudioQueue.c`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/RtpAudioQueue.c) | 718 | — | — | — |
| [`SdpGenerator.c`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/SdpGenerator.c) | 623 | — | — | — |
| [`Connection.c`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/Connection.c) | 542 | — | — | — |
| [`Platform.c`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/Platform.c) | 504 | — | — | — |
| [`AudioStream.c`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/AudioStream.c) | 477 | — | — | — |
| [`PlatformCrypto.c`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/PlatformCrypto.c) | 471 | — | — | — |
| [`RtspParser.c`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/RtspParser.c) | 431 | — | — | — |
| [`VideoStream.c`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/VideoStream.c) | 418 | — | — | — |
| [`ConnectionTester.c`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/ConnectionTester.c) | 309 | — | — | — |
| [`LinkedBlockingQueue.c`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/LinkedBlockingQueue.c) | 250 | — | — | — |
| [`Input.h`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/Input.h) | 206 | — | — | — |
| [`SimpleStun.c`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/SimpleStun.c) | 195 | — | — | — |
| [`ByteBuffer.c`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/ByteBuffer.c) | 176 | — | — | — |
| [`Limelight-internal.h`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/Limelight-internal.h) | 155 | — | — | — |
| [`Misc.c`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/Misc.c) | 154 | — | — | — |
| [`Platform.h`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/Platform.h) | 152 | — | — | — |
| [`FakeCallbacks.c`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/FakeCallbacks.c) | 151 | — | — | — |
| [`PlatformSockets.h`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/PlatformSockets.h) | 132 | — | — | — |
| [`RecorderCallbacks.c`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/RecorderCallbacks.c) | 103 | — | — | — |
| [`RtpAudioQueue.h`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/RtpAudioQueue.h) | 80 | — | — | — |
| [`PlatformThreads.h`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/PlatformThreads.h) | 74 | — | — | — |
| [`Video.h`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/Video.h) | 71 | — | — | — |
| [`Rtsp.h`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/Rtsp.h) | 68 | — | — | — |
| [`RtpVideoQueue.h`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/RtpVideoQueue.h) | 58 | — | — | — |
| [`PlatformCrypto.h`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/PlatformCrypto.h) | 49 | — | — | — |
| [`LinkedBlockingQueue.h`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/LinkedBlockingQueue.h) | 42 | — | — | — |
| [`ByteBuffer.h`](../../app/src/main/jni/moonlight-core/moonlight-common-c/src/ByteBuffer.h) | 30 | — | — | — |

## `app/src/root/java/com.limelight/binding/input/evdev/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`EvdevCaptureProvider.java`](../../app/src/root/java/com.limelight/binding/input/evdev/EvdevCaptureProvider.java) | 348 | 2.5 | EvdevCaptureProvider | `run()`:38 `run()`:210 `showCursor()`:237 `run()`:242 `hideCursor()`:255 `run()`:260 `enableCapture()`:274 `destroy()`:288 |
| [`EvdevTranslator.java`](../../app/src/root/java/com.limelight/binding/input/evdev/EvdevTranslator.java) | 140 | 2.5 | EvdevTranslator | — |
| [`EvdevReader.java`](../../app/src/root/java/com.limelight/binding/input/evdev/EvdevReader.java) | 59 | 2.5 | EvdevReader | — |
| [`EvdevEvent.java`](../../app/src/root/java/com.limelight/binding/input/evdev/EvdevEvent.java) | 39 | 2.5 | EvdevEvent | — |

## `app/src/test/java/com/limelight/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`VidroComposeScreenshotTest.java`](../../app/src/test/java/com/limelight/VidroComposeScreenshotTest.java) | 606 | 2.5 | VidroComposeScreenshotTest, Montagem | `gradeNoCelular()`:57 `gradeNoTablet()`:71 `homeNoCelular()`:78 `homeNoTablet()`:85 `homeSemLugares()`:92 `catalogoNoCelular()`:99 `catalogoNoTablet()`:106 `catalogoComHostDormindo()`:113 |
| [`VidroScreenshotTest.java`](../../app/src/test/java/com/limelight/VidroScreenshotTest.java) | 387 | 2.5 | VidroScreenshotTest | `telasEmRetrato()`:68 `telasEmPaisagem()`:96 `telasNoTablet()`:121 `telasNoS22Ultra()`:142 |
| [`StartupCrashTest.java`](../../app/src/test/java/com/limelight/StartupCrashTest.java) | 289 | 2.5 | StartupCrashTest | `setUp()`:39 `testNativeLibraryLoadingFailure()`:56 `testGLSurfaceViewInitialization()`:70 `testPreferenceConfigurationCrash()`:84 `testUiHelperCrash()`:95 `testComputerManagerServiceBinding()`:108 `testSharedPreferencesCorruption()`:121 `testMissingRequiredIntentExtras()`:141 |
| [`StartupTest.java`](../../app/src/test/java/com/limelight/StartupTest.java) | 235 | 2.5 | StartupTest | `setUp()`:37 `testApplicationStartup()`:54 `testPcViewActivityCreation()`:67 `testPcViewActivityWithIntent()`:75 `testAppViewActivityCreation()`:89 `testProfilesManagerFileSystemAccess()`:101 `testMissingPermissions()`:115 `testCorruptedProfilesFile()`:133 |
| [`PosterContentProviderTest.java`](../../app/src/test/java/com/limelight/PosterContentProviderTest.java) | 173 | 2.5 | PosterContentProviderTest | `setUp()`:39 `legitimateBoxArtIsServed()`:77 `traversalCannotReachAFileOutsideTheBoxArtDirectory()`:91 `traversalShapedUuidsAreRejected()`:113 `nonUuidSegmentsAreRejected()`:120 `nonNumericAppIdIsRejectedWithoutCrashing()`:127 `unexpectedPathsAreRejected()`:135 `writeModesAreRefused()`:154 |
| [`SimpleStartupTest.java`](../../app/src/test/java/com/limelight/SimpleStartupTest.java) | 160 | 2.5 | SimpleStartupTest | `setUp()`:31 `testApplicationCreation()`:48 `testApplicationOnCreate()`:55 `testProfilesManagerSingleton()`:77 `testProfilesManagerLoad()`:88 `testProfilesManagerSave()`:102 `testContextFileAccess()`:117 `testNullContextHandling()`:135 |
| [`ModificadoresPresosTest.java`](../../app/src/test/java/com/limelight/ModificadoresPresosTest.java) | 108 | 2.5 | ModificadoresPresosTest | `soltarTodosDeixaOEstadoEmZero()`:58 `cadaKeyUpLevaOEstadoQueResta()`:69 `semNenhumModificadorNaoHaOQueSoltar()`:84 `oEstadoLocalNaoSobreviveAoReset()`:95 |
| [`PcViewEmptyStateTest.java`](../../app/src/test/java/com/limelight/PcViewEmptyStateTest.java) | 91 | 2.5 | PcViewEmptyStateTest | `semNenhumHost_aHomeSobe()`:83 |
| [`SemBecoSemSaidaTest.java`](../../app/src/test/java/com/limelight/SemBecoSemSaidaTest.java) | 77 | 2.5 | SemBecoSemSaidaTest | `tecladoCompleto_temSaidaPropria()`:52 `barraDeComposicao_temSaidaPropria()`:63 `folhaDeAjustes_temSaidaPropria()`:71 |
| [`OrdemDoVoltarTest.java`](../../app/src/test/java/com/limelight/OrdemDoVoltarTest.java) | 60 | 2.5 | OrdemDoVoltarTest | `cascaNaFrenteDecideSozinha()`:25 `gavetaAbertaFechaAntesDeQualquerCoisaAbrir()`:31 `gavetaFechadaComAtalhoLigadoAbreOsControles()`:38 `semAtalhoOVoltarConvocaACascaEmVezDeEncerrar()`:43 `semVideoNenhumOSistemaDecide()`:49 `aGavetaVenceOAtalhoMesmoSemVideo()`:54 |
| [`LayoutInflationTest.java`](../../app/src/test/java/com/limelight/LayoutInflationTest.java) | 58 | 2.5 | LayoutInflationTest | `allLayoutsInflateSuccessfully()`:26 |
| [`TestLogSuppressor.java`](../../app/src/test/java/com/limelight/TestLogSuppressor.java) | 37 | 15.5 | TestLogSuppressor | `println()`:23 `print()`:30 |
| [`ProfileTestHelper.java`](../../app/src/test/java/com/limelight/ProfileTestHelper.java) | 1 | — | — | — |

## `app/src/test/java/com/limelight/binding/audio/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`AudioTrackPlanTest.java`](../../app/src/test/java/com/limelight/binding/audio/AudioTrackPlanTest.java) | 262 | 2.5 | AudioTrackPlanTest | `estereo_usaOsDoisCanaisDaFrente()`:40 `quatroCanais_saoFlFrCentroLfe_naoAsTraseiras()`:46 `cincoPontoUm_bateComOValorDoAndroid()`:53 `setePontoUm_bateComAConstanteQueEstavaChumbada()`:59 `cadaCanalNovoSoAcrescentaBit()`:67 `contagemForaDoProtocolo_naoTemMascara()`:81 `semObstaculo_tentaBaixaLatenciaPrimeiro()`:97 `comEfeitosLigados_nenhumaTentativaPedeBaixaLatencia()`:118 |

## `app/src/test/java/com/limelight/binding/input/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`TextInputPumpTest.java`](../../app/src/test/java/com/limelight/binding/input/TextInputPumpTest.java) | 345 | 2.5 | TextInputPumpTest, FakeScheduler, RecordingSink | `postDelayed()`:30 `sendText()`:53 `sendBackspaces()`:58 `setUp()`:73 `shortText_isDeliveredWhole()`:88 `multiChunkText_isFullyDelivered()`:98 `queueKeepsWorkingAfterAMultiChunkCommit()`:110 `chunkBoundaryNeverSplitsACodePoint()`:122 |
| [`TouchpadGeometryTest.java`](../../app/src/test/java/com/limelight/binding/input/TouchpadGeometryTest.java) | 112 | 2.5 | TouchpadGeometryTest | `padDeitado_naoEhTratadoComoGirado()`:29 `padQueSeDizEmPe_ehTratadoComoGirado()`:34 `padDeitado_dedoParaDireitaMoveCursorParaDireita()`:40 `padDeitado_dedoParaBaixoMoveCursorParaBaixo()`:49 `padGirado_dedoParaDireitaAindaMoveCursorParaDireita()`:60 `padGirado_dedoParaBaixoNaoFazOCursorSubir()`:72 `aPreferenciaDeTrocarEixosInverteADeducao()`:80 `umaVarridaInteiraDoPadAtravessaAImagemInteira()`:88 |
| [`TouchpadCalibrationTest.java`](../../app/src/test/java/com/limelight/binding/input/TouchpadCalibrationTest.java) | 105 | 2.5 | TouchpadCalibrationTest | `padAlinhado_saiIdentidade()`:22 `padGirado90_dedoParaDireitaVemComoY()`:35 `padGirado180_ambosOsSinaisInvertem()`:48 `padEspelhado_soUmEixoInverte()`:55 `gestoCurtoDemais_naoCalibra()`:63 `gestoDiagonalDemais_naoCalibra()`:70 `doisGestosNoMesmoEixo_naoCalibra()`:76 `sobreviveAoIdaEVolta()`:84 |

## `app/src/test/java/com/limelight/binding/input/virtual_controller/keyboard/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`StickyKeyReleaseTest.java`](../../app/src/test/java/com/limelight/binding/input/virtual_controller/keyboard/StickyKeyReleaseTest.java) | 122 | 2.5 | StickyKeyReleaseTest | `setUp()`:56 `onClick()`:74 `onLongClick()`:78 `onRelease()`:82 `fecharOTecladoSoltaATeclaFixada()`:92 `fecharOTecladoNaoMexeEmTeclaQueNaoEstavaFixada()`:105 `ocultamentoTemporarioPreservaAFixacao()`:112 |

## `app/src/test/java/com/limelight/binding/video/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`FramePacingRouteTest.java`](../../app/src/test/java/com/limelight/binding/video/FramePacingRouteTest.java) | 79 | 2.5 | FramePacingRouteTest | `latenciaMinima_usaLatestOnly()`:31 `nenhumOutroModo_usaLatestOnly()`:36 `somenteEquilibrado_usaChoreographer()`:48 `suavidadeELimiteDeFps_naoDescartamQuadroAtrasado()`:56 `nenhumModo_cabeEmDoisCaminhosDeSaida()`:64 |

## `app/src/test/java/com/limelight/nvstream/http/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`HostCapabilitiesTest.java`](../../app/src/test/java/com/limelight/nvstream/http/HostCapabilitiesTest.java) | 160 | 2.5 | HostCapabilitiesTest | `hostSemModeloDePermissoes_naoHabilitaNada()`:33 `qualquerMascaraNegativa_viraDesconhecida()`:48 `hostComPermissoesEsemClipboard_naoOferece()`:60 `permissaoZerada_eDiferenteDeDesconhecida()`:72 `asDuasDirecoesSaoIndependentes()`:83 `apolloCompleto_liberaAsDuas()`:98 `osBitsSaoOsDoEnumDoApollo()`:107 `aMascaraAtravessaIntacta()`:129 |

## `app/src/test/java/com/limelight/profiles/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`OverlayPreferencesTest.java`](../../app/src/test/java/com/limelight/profiles/OverlayPreferencesTest.java) | 129 | 2.5 | OverlayPreferencesTest | `setup()`:37 `overlayPref_CoercesDoubleToInt()`:47 `overlayPref_CoercesDoubleToLong()`:68 `overlayPref_RemembersZoomOptionsBetweenSessions()`:88 |
| [`ProfilesNavigationTest.java`](../../app/src/test/java/com/limelight/profiles/ProfilesNavigationTest.java) | 118 | 2.5 | ProfilesNavigationTest | `clickingProfileButton_launchesProfilesActivity()`:71 `casca_abreComAtalhoParaLugarInexistente_semEstourar()`:100 `profilesActivity_startsWithoutCrash()`:113 |
| [`ProfilesManagerTest.java`](../../app/src/test/java/com/limelight/profiles/ProfilesManagerTest.java) | 116 | 2.5 | ProfilesManagerTest | `setUp()`:35 `tearDown()`:45 `addAndRetrieveProfile()`:50 `setActivePersists()`:58 `updateAndSaveProfile()`:70 `deleteProfile()`:82 `deleteActiveProfile_resetsActive()`:92 |
| [`ProfilesActivityUiTest.java`](../../app/src/test/java/com/limelight/profiles/ProfilesActivityUiTest.java) | 97 | 2.5 | ProfilesActivityUiTest | `setUp()`:45 `aTelaDeCenasSobeComAsCenasCarregadas()`:62 `apagarUmaCenaTiraElaDoGerenciador()`:82 |
| [`ProfilesOverlayTest.java`](../../app/src/test/java/com/limelight/profiles/ProfilesOverlayTest.java) | 97 | 2.5 | ProfilesOverlayTest | `setUp()`:35 `overlaySharedPreferences_returnsPatchedValues()`:44 `overlayPersistsAcrossSessions()`:66 |

## `app/src/test/java/com/limelight/shadows/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`ShadowMoonBridge.java`](../../app/src/test/java/com/limelight/shadows/ShadowMoonBridge.java) | 38 | 2.5 | ShadowMoonBridge, AudioConfiguration | `toInt()`:22 |
| [`ShadowGameManager.java`](../../app/src/test/java/com/limelight/shadows/ShadowGameManager.java) | 22 | 2.5 | ShadowGameManager | `__constructor__()`:14 `setGameState()`:19 |
| [`ShadowBackdropFrameRenderer.java`](../../app/src/test/java/com/limelight/shadows/ShadowBackdropFrameRenderer.java) | 12 | 2.5 | ShadowBackdropFrameRenderer | `run()`:9 |

## `app/src/test/java/com/limelight/ui/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`MolaTest.java`](../../app/src/test/java/com/limelight/ui/MolaTest.java) | 204 | 2.5 | MolaTest, Registro | `aplicar()`:32 `chegaExatamenteNoDestino()`:48 `criticamenteAmortecidasNaoPassamDoDestino()`:73 `oImaPassaDoDestino()`:84 `aMolaEhInvarianteAEscala()`:103 `oArremessoMudaOIma()`:127 `umDestinoNovoNoMeioNaoDescartaOMovimento()`:159 `umQuadroPerdidoNaoViraUmSalto()`:178 |
| [`PaletaDeComandosTest.java`](../../app/src/test/java/com/limelight/ui/PaletaDeComandosTest.java) | 154 | 2.5 | PaletaDeComandosTest | `montar()`:44 `run()`:73 `enterNoCampoNaoExecutaNada()`:84 `aSetaDesceEOEnterPassaAValerParaALinha()`:94 `subirAcimaDaPrimeiraDevolveOEnterAoCampo()`:107 `oFiltroReiniciaASelecao()`:120 `semResultadoNaoSobraSelecaoPendurada()`:136 `asLetrasNuncaSaoDaPaleta()`:145 |
| [`PrazoDaSessaoTest.java`](../../app/src/test/java/com/limelight/ui/PrazoDaSessaoTest.java) | 132 | 2.5 | PrazoDaSessaoTest | `montar()`:37 `run()`:43 `armaAoSairParaACascaEDerrubaNoFim()`:50 `voltarParaOVideoDesarma()`:63 `armarDuasVezesNaoReiniciaAContagem()`:77 `emPiPNaoArma()`:91 `entrarEmPiPDesarmaOQueJaEstavaCorrendo()`:101 `sairDoPiPNaoRearmaSozinho()`:113 |
| [`StreamContainerAspectRatioTest.java`](../../app/src/test/java/com/limelight/ui/StreamContainerAspectRatioTest.java) | 117 | 2.5 | StreamContainerAspectRatioTest | `semTeclado_oVideoUsaAAlturaQueCabe()`:63 `comTeclado_oVideoEncolheEMantemAProporcao()`:72 `comTeclado_oVideoNaoDesaparece()`:82 `aProporcaoNaoMudaConformeOTecladoCresce()`:92 `semProporcaoDefinida_oContainerNaoSeMete()`:108 |
| [`SaidaPorGestoTest.java`](../../app/src/test/java/com/limelight/ui/SaidaPorGestoTest.java) | 70 | 2.5 | SaidaPorGestoTest | `umEsbarraoNaBordaNaoEncerraAPartida()`:26 `umArrastoLentoAteMetadeEncerra()`:34 `umArremessoCurtoEncerra()`:40 `umArremessoParaCimaNuncaEncerra()`:48 `abaixoDoTercoNaoEncerra()`:56 `alturaZeroNaoDivideNemEncerra()`:64 |
| [`StreamViewCommitTextTest.java`](../../app/src/test/java/com/limelight/ui/StreamViewCommitTextTest.java) | 61 | 2.5 | StreamViewCommitTextTest | `setUp()`:25 `commitText_isForwarded_whenEnabled()`:30 `commitText_notForwarded_whenDisabled()`:46 |

## `app/src/test/java/com/limelight/ui/compose/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`EstadoDaHomeTest.java`](../../app/src/test/java/com/limelight/ui/compose/EstadoDaHomeTest.java) | 163 | 2.5 | EstadoDaHomeTest | `respostaRepetidaNaoTrocaALista()`:40 `mudancaDeVerdadeTrocaALista()`:55 `semRespostaSoDepoisDoPrazo()`:71 `hostQueRespondeZeraORelogioDaSonda()`:87 `dormindoExigeMacGuardado()`:103 `aListaFicaEmOrdemDeNome()`:119 `aPortaPadraoNaoEntraNoEndereco()`:129 `removerTiraDaLista()`:145 |
| [`TelaHomeTest.kt`](../../app/src/test/java/com/limelight/ui/compose/TelaHomeTest.kt) | 154 | — | TelaHomeTest | — |
| [`TelaCenasTest.kt`](../../app/src/test/java/com/limelight/ui/compose/TelaCenasTest.kt) | 110 | — | TelaCenasTest | — |

## `app/src/test/java/com/limelight/utils/`

| Arquivo | Linhas | Rank | Tipos | Símbolos-chave |
|---|--:|--:|---|---|
| [`FailureDialogActionTest.java`](../../app/src/test/java/com/limelight/utils/FailureDialogActionTest.java) | 156 | 2.5 | FailureDialogActionTest | `limparFolhasAbertas()`:58 `aAcaoRodaEDeixaATelaDePe()`:84 `run()`:92 `oOkContinuaEncerrandoESemRodarAAcao()`:113 `run()`:121 `folhaSemAcaoNaoGanhaBotaoNovo()`:140 |
| [`NetworkScopeTest.java`](../../app/src/test/java/com/limelight/utils/NetworkScopeTest.java) | 125 | 2.5 | NetworkScopeTest | `faixasPrivadasDeIpv4NaoSaemDaRede()`:24 `oBlocoDe172EhDozeBitsENaoDezesseis()`:33 `oCgnatEhAFaixaDoTailscaleETambemNaoPassaPeloProvedor()`:43 `enderecoPublicoDeIpv4MantemOTeste()`:52 `ipv6DeEnlaceEUniqueLocal()`:59 `ipv6DeEnlaceChegaComColchetesEZona()`:74 `ipv4VestidoDeIpv6SegueSendoOIpv4DeDentro()`:82 `nomesDeEnlaceSaoLocaisEOsDemaisNao()`:88 |
| [`SpinnerDialogCancelTest.java`](../../app/src/test/java/com/limelight/utils/SpinnerDialogCancelTest.java) | 120 | 2.5 | SpinnerDialogCancelTest | `rodaCancelavelAvisaQuemEsperaESegueNaMesmaTela()`:74 `run()`:82 `rodaQueEncerraAActivityContinuaEncerrando()`:102 |
| [`ResumeDecisionTest.java`](../../app/src/test/java/com/limelight/utils/ResumeDecisionTest.java) | 105 | 2.5 | ResumeDecisionTest | `hostComSessaoAberta_oToqueRetoma()`:25 `hostProntoESemNadaRodando_oToqueAbreOCatalogo()`:31 `hostVistoENaoPareado_oToquePareia()`:37 `hostQueNaoRespondeu_caiNoMenu()`:45 `oCartaoSoAnunciaSessaoQuandoOToqueRealmenteRetoma()`:55 `tocarNoJogoQueJaRoda_retomaSemMenu()`:78 `tocarEmOutroJogoComSessaoAberta_abreOMenu()`:84 `semNadaRodando_oToqueInicia()`:92 |
