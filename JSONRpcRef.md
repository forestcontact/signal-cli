# JsonRpc Command & Repsonse Reference
The following are a list of common commands, receive responses, and error responses within the signal cli

# commands 
### send (to number)
`{"jsonrpc":"2.0","method":"send","params":{"recipient":["+YYY"],"message":"MESSAGE"},"id":4}`
#### join group
`{"jsonrpc":"2.0","method":"updateGroup","params":{"groupId":"B64EncodedGroupID"},"id"GROUPID"},"id":4}`
#### send group message
`{"jsonrpc":"2.0","method":"send","params":{"groupId":"B64EncodedGroupId","message":"Hi Group!"},"id":4}`

# receive
#### send: (to number) receipt success
`{"jsonrpc":"2.0","method":"receive","params":{"envelope":{"source":"+0000000000","sourceNumber":"+0000000000","sourceUuid":"UUID","sourceName":"username","sourceDevice":3,"timestamp":1635214902222,"receiptMessage":{"when":1635214902222,"isDelivery":true,"isRead":false,"timestamps":[1635214901175]}}}}`
#### message read receipt
`{"jsonrpc":"2.0","method":"receive","params":{"envelope":{"source":"+0000000000","sourceNumber":"+0000000000","sourceUuid":"UUID","sourceName":"username","sourceDevice":1,"timestamp":1635217743057,"receiptMessage":{"when":1635217743057,"isDelivery":false,"isRead":true,"timestamps":[1635216989928]}}}}`
#### receive message (text only): 
`{"jsonrpc":"2.0","method":"receive","params":{"envelope":{"source":"+0000000000","sourceNumber":"+0000000000","sourceUuid":"UUID","sourceName":"username","sourceDevice":3,"timestamp":1635213315943,"dataMessage":{"timestamp":1635213315943,"message":"hello","expiresInSeconds":0,"viewOnce":false,"mentions":[],"attachments":[],"contacts":[]}}}}`-
#### receive group message
`{"jsonrpc":"2.0","method":"receive","params":{"envelope":{"source":"+0000000000","sourceNumber":"+0000000000","sourceUuid":"UUID","sourceName":"username","sourceDevice":3,"timestamp":1635220676795,"dataMessage":{"timestamp":1635220676795,"message":"Sup peoples","expiresInSeconds":0,"viewOnce":false,"mentions":[],"attachments":[],"contacts":[],"groupInfo":{"groupId":"B64EncodedGroupId","type":"DELIVER"}}}}}`
#### receive message (text + attachments): 
`{"jsonrpc":"2.0","method":"receive","params":{"envelope":{"source":"+0000000000","sourceNumber":"+0000000000","sourceUuid":"UUID","sourceName":"username","sourceDevice":1,"timestamp":1635214142214,"dataMessage":{"timestamp":1635214142214,"message":null,"expiresInSeconds":0,"viewOnce":false,"mentions":[],"attachments":[{"contentType":"video/mp4","filename":null,"id":"2j64BkLuocRJu9heqdlh","size":25782},{"contentType":"image/jpeg","filename":null,"id":"-8v9_rStlVJHrQp91jPq","size":865397}],"contacts":[]}}}}`
#### typing
`{"jsonrpc":"2.0","method":"receive","params":{"envelope":{"source":"+0000000000","sourceNumber":"+0000000000","sourceUuid":"UUID","sourceName":"username","sourceDevice":3,"timestamp":1635211774384,"typingMessage":{"action":"STOPPED","timestamp":1635211774384}}}}`    
#### group invite
`{"jsonrpc":"2.0","method":"receive","params":{"envelope":{"source":"+0000000000","sourceNumber":"+0000000000","sourceUuid":"UUID","sourceName":"username","sourceDevice":3,"timestamp":1635218546209,"dataMessage":{"timestamp":1635218546209,"message":null,"expiresInSeconds":0,"viewOnce":false,"mentions":[],"attachments":[],"contacts":[],"groupInfo":{"groupId":"B64EncodedGroupId","type":"UPDATE"}}}}}`

# errors
#### send: to unregistered user
`{"jsonrpc":"2.0","error":{"code":-32603,"message":"Failed to send message: null (UnregisteredUserException)","data":null},"id":4}`
#### receive broken keystate
`{"jsonrpc":"2.0","method":"receive","params":{"envelope":{"source":null,"sourceNumber":null,"sourceUuid":null,"sourceName":null,"sourceDevice":null,"timestamp":1635211582905},"error":{"message":"org.whispersystems.libsignal.InvalidMessageException: invalid message Message decryption failed","type":"ProtocolInvalidMessageException"}}}`
#### invalid command line input
`{"jsonrpc":"2.0","error":{"code":-32600,"message":"invalid request","data":null},"id":null}`
#### command Not Implemented
`{"jsonrpc":"2.0","error":{"code":-32601,"message":"Method not implemented","data":null},"id":6}`


