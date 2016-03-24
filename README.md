# Dictionary #

Tiny Android application that allows you to translate and view information about words.

![Screen 1](/art/scr01.jpg)
![Screen 2](/art/scr02.jpg)

## Supported languages ##

Everything is backed by [Yandex.Dictionary](https://tech.yandex.com/dictionary/). Check out their [page](https://tech.yandex.com/dictionary/doc/dg/concepts/api-overview-docpage/) to find more information about supported languages and features.

## Configuration ##

Configuration parameters are located in `app/config` folder. Sample debug configuration file is provided, which looks like this:
```properties
baseUrl         = "https://dictionary.yandex.net/api/v1/dicservice.json/"
apiKey          = "myApiKey"
keystore        = path/to/keystore
keystorePasswd  = somePassword
alias           = someAlias
aliasPasswd     = someAliasPassword
```
It's highly recommended to change `apiKey` field with _your_ API key. You can get the free one [here](https://tech.yandex.com/keys/get/?service=dict).

# License #

    Copyright 2016 Igor Talankin

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.