# cute-discord

A super-cute Discord bot backend in Java

## Deployment notes

If using ScyllaDB (I am), you ***NEED*** to be running it with the `--experimental 1` flag for secondary indexes support. IT 
WILL NOT WORK WITHOUT THIS

## Event format

Events pushed into NATS should be structured like this:

```JSON
{
  "t": "type",
  "ts": 1234567890,
  "shard": {
    "id": 1,
    "limit": 123
  },
  "data": {
    "event data": "goes here"
  }
}
```

Note that the `shard` field is OPTIONAL, and is used to signal which Discord shard the event came from.

## Websocket API

Websocket API messages are formatted as follows:

```JSON
{
  "op": 1,
  "t": "event type (may not be provided, depending on the op)",
  "d": {
    "op data": "goes here"
  },
  "ts": 1234567890
}
```

WS opcodes:

| id | name      |
|--- | ----------|
|  0 | IDENTIFY  |
|  1 | HEARTBEAT |
