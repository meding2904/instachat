package com.instachat.app.data.local.entity

/**
 * Who authored a message. FRIEND messages are produced by the simulator.
 */
enum class Sender { ME, FRIEND }

enum class MessageType { TEXT, IMAGE, VOICE }

enum class MessageStatus { SENT, DELIVERED, SEEN }
