package org.acme.endpoint;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
public record ErrorMessages(List<String> messages) {}
