package com.transport.transport.dto;

public record InviterEmailRequest(
        String email,
        String invitePar,
        String inviterNom,
        String inviterPrenom
) {}
