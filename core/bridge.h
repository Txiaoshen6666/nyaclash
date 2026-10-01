#ifndef NYACLASH_BRIDGE_H
#define NYACLASH_BRIDGE_H

/*
 * Functions implemented in bridge.c and called from Go.
 *
 * They forward to the Java `TunInterface` object that was handed to
 * nativeStartTun, so mihomo can protect its outbound sockets (VpnService.protect)
 * and resolve the source UID of a TUN connection.
 */
int jni_protect(int fd);
int jni_query_socket_uid(int protocol, const char *source, const char *target);

#endif /* NYACLASH_BRIDGE_H */
