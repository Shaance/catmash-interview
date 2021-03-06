package com.shaance.catmashinterview.connector;

import com.shaance.catmashinterview.entity.Cat;

import java.util.stream.Stream;

public interface CatConnector {


	/**
	 * @param uri place where we can retrieve cat
	 * @return Stream of cats from the URI
	 */
	Stream<Cat> getCatsFromStringURI(String uri);

}
