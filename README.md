# Netifi Java SDK

<a href='https://travis-ci.org/netifi/netifi-sdk-java'><img src='https://travis-ci.org/netifi/netifi-sdk-java.svg?branch=master'></a>

The [Netifi](http://www.getnetifi.com/) SDK for Java connects to Netifi's router using the [RSocket](http://rsocket.io/) protocol. It serves as a drop-in replacement to the RSocketFactory, and provides a transparent routing layer.

Each connection to the Netifi Router is a identified by a unique destination id. Each connection also belongs to a group. The destination id is string up to 255 characters, and a group is string up to 255. You can either
route calls to a single destination, or you can route calls to a group. If you route calls to a group the Netifi Router will load balance automatically.

## Proteus Example
Link to an [example](https://github.com/netifi/netifi-sdk-java-examples) using [Proteus](https://github.com/netifi/proteus-java) to send requests, but anything that uses an RSocket can be used.

## Bugs and Feedback

For bugs, questions, and discussions please use the [Github Issues](https://github.com/netifi/netifi-sdk-java/issues).

## License
Copyright 2017 Netifi Inc.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
