package com.shaance.catmashinterview.service;

import com.shaance.catmashinterview.dao.CatDao;
import com.shaance.catmashinterview.dao.CatMashRecordDao;
import com.shaance.catmashinterview.dto.CatDto;
import com.shaance.catmashinterview.dto.CatWithNumberOfVotesDto;
import com.shaance.catmashinterview.dto.CatWithWinningRatioDto;
import com.shaance.catmashinterview.entity.Cat;
import com.shaance.catmashinterview.entity.CatMashRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CatMashStatisticServiceImpl implements CatMashStatisticService {

	private CatMashRecordDao catMashRecordDao;
	private CatDao catDao;

	@Autowired
	public CatMashStatisticServiceImpl(CatMashRecordDao catMashRecordDao, CatDao catDao) {
		this.catMashRecordDao = catMashRecordDao;
		this.catDao = catDao;
	}

	@Override
	public Flux<CatWithNumberOfVotesDto> getCatsWithAllTimeVotesInDescOrder() {

		return getMostVotedCatsWithPredicate(catMashRecord -> true);
	}

	@Override
	public Flux<CatWithNumberOfVotesDto> getCatsWithTodayVotesInDescOrder() {
		return getMostVotedCatsWithPredicate(catMashRecord ->
				catMashRecord.getLocalDateTime().toLocalDate().toEpochDay() == LocalDate.now().toEpochDay());
	}

	@Override
	public Flux<CatWithWinningRatioDto> getCatsWithAllTimeWinningRatioInDescOrder() {
		return getCatWithWinningRatioFluxWithPredicate(catMashRecord -> true);
	}

	@Override
	public Flux<CatWithWinningRatioDto> getCatsWithTodayWinningRatioInDescOrder() {
		return getCatWithWinningRatioFluxWithPredicate(catMashRecord ->
				catMashRecord.getLocalDateTime().toLocalDate().toEpochDay() == LocalDate.now().toEpochDay());
	}

	private Flux<CatWithNumberOfVotesDto> getMostVotedCatsWithPredicate(Predicate<CatMashRecord> filterCondition) {

		return catMashRecordDao.findAll()
				.filter(filterCondition)
				.map(CatMashRecord::getWinnerCatId)
				.collect(Collectors.groupingBy(String::valueOf, Collectors.counting()))
				.flatMapMany(stringLongMap -> catDao.findAll()
								.map(cat -> new CatWithNumberOfVotesDto(new CatDto(cat.getId(), cat.getUrl().toString()),
										stringLongMap.getOrDefault(cat.getId(), 0L)))
								.sort(Comparator.comparing(CatWithNumberOfVotesDto::getVotes).reversed()));

	}

	private Flux<CatWithWinningRatioDto> getCatWithWinningRatioFluxWithPredicate(Predicate<CatMashRecord> filterCondition) {

		//catId, numberOfWins, numberOfLoses
		Map<String, Pair<Float, Float>> catStatsMap = new HashMap<>();

		return catMashRecordDao.findAll()
				.filter(filterCondition)
				.doOnNext(catMashRecord -> fillCatStatsMap(catMashRecord, catStatsMap))
				.thenMany(catDao.findAll()
						.map(cat -> catToCatWithWinningRatioDto(cat, catStatsMap))
						.sort(Comparator.comparing(CatWithWinningRatioDto::getWinningRatio).reversed())
				);
	}

	private CatWithWinningRatioDto catToCatWithWinningRatioDto(Cat cat, Map<String, Pair<Float, Float>> catStatsMap){

		if (catStatsMap.containsKey(cat.getId())) {
			Pair<Float, Float> floatPair = catStatsMap.get(cat.getId());
			float winningRatio = floatPair.getFirst() / (floatPair.getFirst() + floatPair.getSecond());
			return new CatWithWinningRatioDto(new CatDto(cat.getId(), cat.getUrl().toString()), winningRatio);
		} else {
			return new CatWithWinningRatioDto(new CatDto(cat.getId(), cat.getUrl().toString()), -1f);
		}

	}

	private void fillCatStatsMap(CatMashRecord catMashRecord, Map<String, Pair<Float, Float>> catStatsMap){

		Consumer<CatMashRecord> consumer = (c) -> {
			String winnerCatId = c.getWinnerCatId();
			String loserCatId = c.getLoserCatId();

			if (catStatsMap.containsKey(winnerCatId)) {
				Pair<Float, Float> pair = catStatsMap.get(winnerCatId);
				catStatsMap.put(winnerCatId, Pair.of(pair.getFirst() + 1f, pair.getSecond()));
			} else {
				catStatsMap.put(winnerCatId, Pair.of(1f, 0f));
			}

			if (catStatsMap.containsKey(loserCatId)) {
				Pair<Float, Float> pair = catStatsMap.get(loserCatId);
				catStatsMap.put(loserCatId, Pair.of(pair.getFirst(), pair.getSecond() + 1f));
			} else {
				catStatsMap.put(loserCatId, Pair.of(0f, 1f));
			}
		};

		consumer.accept(catMashRecord);

	}

}
