package com.relative.chat.bot.ia.domain.knowledge;
 import com.relative.chat.bot.ia.domain.common.*;
 import com.relative.chat.bot.ia.domain.identity.*;
 import java.util.*;
 public final class Kb{
private final Id<Kb> id;
 private final Id<Client> clientId;
 private String name;
 private String description;
 public Kb(Id<Kb> id, Id<Client> clientId, String name, String description){
this.id=Objects.requireNonNull(id);
 this.clientId=Objects.requireNonNull(clientId);
 if(name==null||name.isBlank()) throw new DomainException("KB.name requerido");
 this.name=name;
 this.description=description;
 }
public Id<Kb> id(){return id;
}
public Id<Client> clientId(){return clientId;
}
public String name(){return name;
}
public String description(){return description;
}
}