{
  "targets": [
    {
      "target_name": "console_bridge",
      "sources": [ "console_bridge.cpp" ],
      "conditions": [
        ["OS=='win'", {
          "libraries": [
            "-lkernel32.lib",
            "-luser32.lib"
          ],
          "defines": [
            "WIN32_LEAN_AND_MEAN",
            "_WIN32_WINNT=0x0601"
          ]
        }]
      ],
      "configurations": {
        "Release": {
          "msvs_settings": {
            "VCCLCompilerTool": {
              "RuntimeLibrary": 2,
              "WarningLevel": 3
            }
          }
        }
      }
    }
  ]
}
