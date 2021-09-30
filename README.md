# otoroshi-verifier-group-emulator

A plugin allowing to emulate a verifier group feature.

this plugin use service descriptor plugin configuration and the global configuration to emulate a group verifier.

The plugin instance config as two attribut, 
- `strict` to make this verifer strict
- `grouId` to lookup for member verifier 

The plugin global configuration as two main attribut:
- `strict` the default plugin instance value for force, the default value is false
- `groups` an Object (map) this the differnts grouId as key, and an array of verifier id as value.

***This plugin is still experimental, and is tester against Otoroshi 1.5.0***
