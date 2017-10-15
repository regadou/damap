package org.regadou.damai;

public interface Namespace {

   String getUri();

   String getPrefix();

   Repository<Resource> getRepository();
}
