# mewna-discord

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

Note that the `shard` field is OPTIONAL, and is used to signal which
Discord shard the event came from.

## Mewna Epoch

This will matter later

1518566400 * 1000 - February 14th, 2018 @ 00:00 UTC. milliseconds.

## Accounts endpoint

`/data/account/:id`

You can `GET` this endpoint, or `POST` data to `/update`. `POST` payload should
 look like

```Javascript
{
  // Retrieved from OAuth
  "email": "me@me.me",
  // If creating an OAuth account, this field can be empty *for now*
  // Later on this will have to be filled in
  "username": "",
  // Can just be OAuth account name for now
  "displayName": "",
  // ID of OAuth account being linked to this account
  // Used for querying on etc
  "discordAccountId": ""
}
```

If no `id` field is supplied, a new one will be generated. To make sure
that you don't create many duplicate accounts, you should first `GET`
the `/data/account/links/discord/:id` endpoint to see if a linked
account already exists; if it does, it'll return a single string which is
the existing account's id.