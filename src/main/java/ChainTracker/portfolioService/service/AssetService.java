package ChainTracker.portfolioService.service;

import ChainTracker.portfolioService.dto.MarketResponse;
import ChainTracker.portfolioService.model.Asset;
import ChainTracker.portfolioService.repository.AssetRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class AssetService {
    private final RedisTemplate<String,Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private AssetRepository assetRepository;
    private DataService dataService;


    public Asset findOrCreate(MarketResponse qoute){
           return assetRepository.findByAssetId(qoute.id()).orElseGet(() -> {
            Asset asset = new Asset();
            asset.setAssetId(qoute.id());
            asset.setName(qoute.name());
            asset.setSymbol(qoute.symbol());
            return assetRepository.save(asset);
        });
    }

    public MarketResponse getQuote(String assetId){
        Object cached = redisTemplate.opsForValue().get("coin:" + assetId);
        if (cached != null){
            return objectMapper.convertValue(cached, MarketResponse.class);
        }
        MarketResponse coin = dataService.getCoin(assetId);
        return coin;
    };

    /**
     * Versao em lote do getQuote: uma ida ao Redis para todos os ids de uma vez.
     *
     * O getQuote paga uma chamada por ativo. Numa carteira de 20 posicoes sao 20
     * idas ao Redis so para montar a tela; aqui e uma, via multiGet.
     *
     * Os ids que nao estiverem no cache sao buscados um a um, porque essa parte
     * sai para a rede e o outro servico nao expoe um endpoint em lote para
     * moedas (o /prices usa outra chave, coin:simple:, nao esta).
     *
     * Ids sem preco simplesmente nao entram no mapa — quem chama decide o que
     * fazer com a ausencia, em vez de receber um null disfarcado de resposta.
     */
    public Map<String, MarketResponse> getQuotes(Collection<String> assetIds){
        if (assetIds.isEmpty()){
            return Map.of();
        }

        List<String> ids = List.copyOf(assetIds);
        List<String> keys = ids.stream().map(id -> "coin:" + id).toList();

        List<Object> cached = redisTemplate.opsForValue().multiGet(keys);

        Map<String, MarketResponse> quotes = new HashMap<>();
        List<String> cold = new ArrayList<>();

        for (int i = 0; i < ids.size(); i++){
            Object value = cached == null ? null : cached.get(i);
            if (value == null){
                cold.add(ids.get(i));
            } else {
                quotes.put(ids.get(i), objectMapper.convertValue(value, MarketResponse.class));
            }
        }

        for (String id : cold){
            MarketResponse quote = dataService.getCoin(id);
            if (quote != null){
                quotes.put(id, quote);
            }
        }

        return quotes;
    }

    public Asset findByAssetId(String assetId){
        return assetRepository.findByAssetId(assetId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
