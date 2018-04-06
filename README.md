# Connection with Bitrix

## Goal

User's dialog should be transferred to Bitrix support and continue with a human operator. 

## Usage

The plugin is called using a URL of this type ```http://plugins.miniapps.run/bitrix?....```. For instance:

```<page version="2.0">
  <div>
    Hello!
  </div>
 
  <navigation>
    <link pageId="http://plugins.miniapps.run/bitrix?domain=eyeline.bitrix24.ru&amp;locale=ru&amp;back_page=http%3A%2F%2Fhosting-api-test.eyeline.mobi%2Findex%3Fsid%3D265%26amp%3Bsver%3D784%26amp%3Bpid%3D21%26amp%3Banswer%3Dback_page">
      Ask our support operator
    </link>
  </navigation>
</page>
```

## Parameters
|Parameter 	|Mandatory	|Description 		|
|:--------------|:-------------:|:----------------------|
|domain		|Yes		|Bitrix local domain.	|
|back_page	|Yes		|The address of the page where the user should be landed after communicating with Bitrix. The Home button is always available to the user when communicating with the bot, no matter which messenger is in use.|
|locale		|Yes		|The language of communication with the bot.|
