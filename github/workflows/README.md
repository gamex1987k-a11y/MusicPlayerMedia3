# MusicPlayerMedia3

Proyecto de ejemplo: reproductor de música con Media3 (ExoPlayer), MediaSessionService y PlayerNotificationManager con carga de artwork.

## Requisitos
- Android Studio (Electric Eel o superior recomendado)
- SDK Android 34
- Dispositivo real con archivos de audio para pruebas

## Importar
1. Abre la carpeta MusicPlayerMedia3 en Android Studio (File > Open).
2. Espera a que Gradle sincronice.
3. Ejecuta en un dispositivo real.

## Notas
- Solicita READ_EXTERNAL_STORAGE en tiempo de ejecución.
- En Android 11+ considera usar el picker para evitar permisos globales.
- Mejoras recomendadas: manejo de audio focus, cola de reproducción, persistencia de posición.
