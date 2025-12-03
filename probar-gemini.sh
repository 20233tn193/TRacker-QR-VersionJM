#!/bin/bash

# Script para probar Gemini después de actualizar en Firestore
# Ejecutar DESPUÉS de agregar estado al cliente en Firebase Console

BASE_URL="http://localhost:8080/api"

echo "🧪 PRUEBA DE GEMINI - CÁLCULO DE RUTA OPTIMIZADA"
echo "================================================"
echo ""

# Login admin
echo "🔑 Login como admin..."
TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@gmail.com","password":"admin1234"}' | jq -r '.data.token')

if [ -z "$TOKEN" ] || [ "$TOKEN" == "null" ]; then
    echo "❌ Admin bloqueado. Desbloquea en Firestore:"
    echo "   Documento: RGXzZwDiBqdeyQw7avwF"
    echo "   Cambiar: bloqueadoHasta → null, intentosFallidos → 0"
    exit 1
fi

echo "✅ Token obtenido"
echo ""

# Crear paquete
echo "📦 Creando paquete (Gemini calculará ruta)..."
echo "   Cliente: cliente@gmail.com"
echo "   Destino: Mérida, Yucatán"
echo ""

PAQUETE=$(curl -s -X POST "$BASE_URL/paquetes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "descripcion": "Prueba Gemini - Ruta Optimizada IA",
    "clienteEmail": "cliente@gmail.com",
    "direccionDestino": "Calle 60 #123, Mérida, Yucatán"
  }')

SUCCESS=$(echo $PAQUETE | jq -r '.success')

if [ "$SUCCESS" != "true" ]; then
    echo "❌ Error al crear paquete:"
    echo "$PAQUETE" | jq '.'
    echo ""
    echo "Si dice 'El cliente no tiene un estado registrado':"
    echo "   Agrega en Firestore → usuarios → lUr5IAHBiD7sYSb37jMG"
    echo "   Campo: estado → 'Yucatán'"
    exit 1
fi

# Mostrar ruta
echo "✅ Paquete creado"
echo ""
echo "📊 RUTA CALCULADA POR GEMINI:"
echo ""

CODIGO_QR=$(echo $PAQUETE | jq -r '.data.codigoQR')
NUM_ESTADOS=$(echo $PAQUETE | jq -r '.data.estadosRuta | length')

echo "$PAQUETE" | jq -r '.data.estadosRuta[]?' | nl -w2 -s'. '

echo ""

if [ "$NUM_ESTADOS" -gt "2" ]; then
    echo "✅ ¡GEMINI FUNCIONÓ! ($NUM_ESTADOS estados)"
    echo "   La IA calculó una ruta optimizada con centros intermedios"
else
    echo "⚠️  Ruta simple ($NUM_ESTADOS estados)"
    echo "   Verificar API key de Gemini"
fi

echo ""
echo "📦 Código QR: $CODIGO_QR"
echo "🌐 Swagger: http://localhost:8080/api/swagger-ui.html"
echo ""

