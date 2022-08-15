package com.baeldung.jwt.api;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.baeldung.jwt.resource.Foo;

@RestController
@RequestMapping(value = "/foos")
public class FooController {

    private static final Logger logger = LoggerFactory.getLogger(FooController.class);

    @GetMapping(value = "/{id}")
    public Foo findOne(@PathVariable Long id) {
        return new Foo(Long.parseLong(randomNumeric(2)), randomAlphabetic(4));
    }

    @GetMapping
    public List<Foo> findAll() {
        List<Foo> fooList = new ArrayList<Foo>();
        fooList.add(new Foo(Long.parseLong(randomNumeric(2)), randomAlphabetic(4)));
        fooList.add(new Foo(Long.parseLong(randomNumeric(2)), randomAlphabetic(4)));
        fooList.add(new Foo(Long.parseLong(randomNumeric(2)), randomAlphabetic(4)));
        return fooList;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public void create(@RequestBody Foo newFoo) {
        logger.info("Foo created");
    }

}