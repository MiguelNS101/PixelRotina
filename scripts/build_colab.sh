#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# PixelRotina — Script de Compilacao Automatizada para Google Colab / Ubuntu
# Instala JDK 21, Gradle 9.3.1, Android SDK 36.1, configura keystores e gera APKs.
# ==============================================================================

BUILD_TYPE="${1:-both}"          # debug | release | both
RUN_TESTS="${2:-true}"           # true | false
PROJECT_DIR="$(pwd)"

JDK_DIR="/usr/lib/jvm/java-21-openjdk-amd64"
GRADLE_VERSION="9.3.1"
GRADLE_DIR="/opt/gradle-${GRADLE_VERSION}"
ANDROID_SDK_DIR="/opt/android-sdk"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"

echo "============================================================"
echo " PixelRotina — Preparando ambiente Android no Google Colab"
echo " Diretorio: ${PROJECT_DIR}"
echo " Build: ${BUILD_TYPE} | Testes Robolectric: ${RUN_TESTS}"
echo "============================================================"

# 1. Instalar JDK 21 e utilitarios basicos
if [ ! -d "${JDK_DIR}" ]; then
  echo "[1/6] Instalando OpenJDK 21..."
  sudo apt-get update -qq
  sudo apt-get install -y -qq openjdk-21-jdk unzip wget curl >/dev/null
else
  echo "[1/6] OpenJDK 21 ja instalado."
fi

export JAVA_HOME="${JDK_DIR}"
export PATH="${JAVA_HOME}/bin:${PATH}"
java -version

# 2. Instalar Gradle 9.3.1
if [ ! -x "${GRADLE_DIR}/bin/gradle" ]; then
  echo "[2/6] Baixando e instalando Gradle ${GRADLE_VERSION}..."
  wget -q "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -O /tmp/gradle.zip
  sudo unzip -q -o /tmp/gradle.zip -d /opt
  rm -f /tmp/gradle.zip
else
  echo "[2/6] Gradle ${GRADLE_VERSION} ja instalado."
fi

export GRADLE_HOME="${GRADLE_DIR}"
export PATH="${GRADLE_HOME}/bin:${PATH}"
gradle --version | head -n 5

# 3. Instalar Android SDK Command-line Tools & API 36 / 36.1
if [ ! -x "${ANDROID_SDK_DIR}/cmdline-tools/latest/bin/sdkmanager" ]; then
  echo "[3/6] Instalando Android SDK Command-line Tools..."
  sudo mkdir -p "${ANDROID_SDK_DIR}/cmdline-tools"
  wget -q "${CMDLINE_TOOLS_URL}" -O /tmp/cmdline-tools.zip
  sudo unzip -q -o /tmp/cmdline-tools.zip -d "${ANDROID_SDK_DIR}/cmdline-tools"
  sudo rm -rf "${ANDROID_SDK_DIR}/cmdline-tools/latest"
  sudo mv "${ANDROID_SDK_DIR}/cmdline-tools/cmdline-tools" "${ANDROID_SDK_DIR}/cmdline-tools/latest"
  sudo chmod -R a+rwx "${ANDROID_SDK_DIR}"
  rm -f /tmp/cmdline-tools.zip
else
  echo "[3/6] Android SDK Command-line Tools ja instalado."
fi

export ANDROID_HOME="${ANDROID_SDK_DIR}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_DIR}"
export PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

if [ ! -d "${ANDROID_SDK_DIR}/platforms/android-36" ] || [ ! -d "${ANDROID_SDK_DIR}/build-tools/36.0.0" ]; then
  echo "[3/6] Aceitando licencas e instalando Android SDK (API 36, 36.1 e Build-Tools 36.0.0)..."
  yes | sdkmanager --licenses >/dev/null 2>&1 || true
  sdkmanager "platform-tools" "platforms;android-36" "platforms;android-36.1" "build-tools;36.0.0"
else
  echo "[3/6] Plataformas Android 36/36.1 e Build-Tools ja instalados."
fi

# 4. Preparar .env e Keystores (Debug e Release)
echo "[4/6] Preparando .env e keystores de assinatura..."
if [ ! -f "${PROJECT_DIR}/.env" ] && [ -f "${PROJECT_DIR}/.env.example" ]; then
  cp "${PROJECT_DIR}/.env.example" "${PROJECT_DIR}/.env"
fi

if [ ! -f "${PROJECT_DIR}/debug.keystore" ]; then
  if [ -f "${PROJECT_DIR}/debug.keystore.base64" ]; then
    base64 --decode "${PROJECT_DIR}/debug.keystore.base64" > "${PROJECT_DIR}/debug.keystore"
    echo " -> debug.keystore restaurado a partir de debug.keystore.base64"
  else
    keytool -genkeypair -v \
      -keystore "${PROJECT_DIR}/debug.keystore" \
      -storepass android \
      -alias androiddebugkey \
      -keypass android \
      -keyalg RSA \
      -keysize 2048 \
      -validity 10000 \
      -dname "CN=Android Debug,O=Android,C=US" >/dev/null 2>&1
    echo " -> debug.keystore gerado automaticamente"
  fi
fi

export KEYSTORE_PATH="${KEYSTORE_PATH:-${PROJECT_DIR}/my-upload-key.jks}"
export STORE_PASSWORD="${STORE_PASSWORD:-pixelrotina123}"
export KEY_PASSWORD="${KEY_PASSWORD:-pixelrotina123}"

if [ "${BUILD_TYPE}" = "release" ] || [ "${BUILD_TYPE}" = "both" ]; then
  if [ ! -f "${KEYSTORE_PATH}" ]; then
    keytool -genkeypair -v \
      -keystore "${KEYSTORE_PATH}" \
      -storepass "${STORE_PASSWORD}" \
      -alias upload \
      -keypass "${KEY_PASSWORD}" \
      -keyalg RSA \
      -keysize 2048 \
      -validity 10000 \
      -dname "CN=PixelRotina Release,OU=Mobile,O=PixelRotina,L=SaoPaulo,ST=SP,C=BR" >/dev/null 2>&1
    echo " -> Keystore de release criada em ${KEYSTORE_PATH}"
  else
    echo " -> Usando keystore de release existente em ${KEYSTORE_PATH}"
  fi
fi

# 5. Executar testes unitarios (Robolectric) se habilitado
if [ "${RUN_TESTS}" = "true" ]; then
  echo "[5/6] Executando testes unitarios (Robolectric)..."
  gradle :app:testDebugUnitTest --no-daemon
else
  echo "[5/6] Testes unitarios ignorados conforme configuracao."
fi

# 6. Compilar APK(s)
echo "[6/6] Compilando APK(s) (${BUILD_TYPE})..."
mkdir -p "${PROJECT_DIR}/dist"

case "${BUILD_TYPE}" in
  debug)
    gradle assembleDebug --no-daemon
    cp "${PROJECT_DIR}/app/build/outputs/apk/debug/app-debug.apk" "${PROJECT_DIR}/dist/PixelRotina-debug.apk"
    ;;
  release)
    gradle assembleRelease --no-daemon
    cp "${PROJECT_DIR}/app/build/outputs/apk/release/app-release.apk" "${PROJECT_DIR}/dist/PixelRotina-release.apk"
    ;;
  both|*)
    gradle assembleDebug assembleRelease --no-daemon
    cp "${PROJECT_DIR}/app/build/outputs/apk/debug/app-debug.apk" "${PROJECT_DIR}/dist/PixelRotina-debug.apk"
    cp "${PROJECT_DIR}/app/build/outputs/apk/release/app-release.apk" "${PROJECT_DIR}/dist/PixelRotina-release.apk"
    ;;
esac

echo "============================================================"
echo " Compilacao concluida com sucesso! Arquivos gerados em dist/:"
ls -lh "${PROJECT_DIR}/dist/"
echo "============================================================"
