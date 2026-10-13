## Visage: Culling and Virtual Entities

Visage is a library that provides API for Culling, aswell as wrappers for Virtual Entities.

### Ways to use:
Adding as dependency will NOT be explained here

1. Shade the jar into your plugin and relocate it
2. if using paper-plugin.yml use PluginLoader to download the library at runtime
3. if using plugin.yml use the libraries section e.g
```yml
libraries:
- io.github.darksoulq:Visage:1.0.9
```

### Note on Visual Studio Code and Eclipse Users (Ignore if using Intellij):
Using this library you will notice warnings and errors in the IDE, while these do not affect compilation they do distrupt experience, therefore you need to download the provided MinecraftStubs jar from Releases, and add that as a library aswell (do NOT shade this or even include by plugin/paper-plugin.yml! as that will cause a crash)
