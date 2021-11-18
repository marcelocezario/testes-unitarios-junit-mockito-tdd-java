package br.ce.wcaquino.servicos;

import static br.ce.wcaquino.builders.FilmeBuilder.umFilme;
import static br.ce.wcaquino.builders.FilmeBuilder.umFilmeSemEstoque;
import static br.ce.wcaquino.builders.UsuarioBuilder.umUsuario;
import static br.ce.wcaquino.matchers.MatchersProprios.caiNumaSegunda;
import static br.ce.wcaquino.matchers.MatchersProprios.ehHoje;
import static br.ce.wcaquino.matchers.MatchersProprios.ehHojeComDiferencaDias;
import static br.ce.wcaquino.utils.DataUtils.isMesmaData;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import br.ce.wcaquino.builders.LocacaoBuilder;
import br.ce.wcaquino.daos.LocacaoDAO;
import br.ce.wcaquino.entidades.Filme;
import br.ce.wcaquino.entidades.Locacao;
import br.ce.wcaquino.entidades.Usuario;
import br.ce.wcaquino.exceptions.FilmeSemEstoqueException;
import br.ce.wcaquino.exceptions.LocadoraException;
import br.ce.wcaquino.utils.DataUtils;

public class LocacaoServiceTest {

	@InjectMocks
	@Spy
	private LocacaoService service;

	@Mock
	private LocacaoDAO dao;
	@Mock
	private SPCService spc;
	@Mock
	private EmailService emailService;

	@Rule
	public ErrorCollector error = new ErrorCollector();

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		System.out.println("Iniciando 2...");
		CalculadoraTest.ordem.append("2");
	}

	@After
	public void tearDown() {
		System.out.println("Finalizando 2...");
	}

	@AfterClass
	public static void tearDownClass() {
		System.out.println(CalculadoraTest.ordem.toString());
	}

	@Test
	public void deveAlugarFilme() throws Exception {

		// cenário
		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilme().comValor(5.0).agora());

		Mockito.doReturn(DataUtils.obterData(15, 11, 2021)).when(service).obterData();

		// ação
		Locacao locacao = service.alugarFilme(usuario, filmes);

		// verificação
		error.checkThat(locacao.getValor(), is(equalTo(5.0)));
		error.checkThat(isMesmaData(locacao.getDataLocacao(), DataUtils.obterData(15, 11, 2021)), is(true));
		error.checkThat(isMesmaData(locacao.getDataRetorno(), DataUtils.obterData(16, 11, 2021)), is(true));

	}

	@Test(expected = FilmeSemEstoqueException.class)
	public void naoDeveAlugarFilmeSemEstoque() throws Exception {

		// cenário
		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilmeSemEstoque().agora());

		// ação
		service.alugarFilme(usuario, filmes);
	}

	@Test
	public void naoDeveAlugarFilmeSemUsuario() throws FilmeSemEstoqueException {
		// cenario
		List<Filme> filmes = Arrays.asList(umFilme().agora());

		// acao
		try {
			service.alugarFilme(null, filmes);
			Assert.fail();
		} catch (LocadoraException e) {
			Assert.assertThat(e.getMessage(), is("Usuário vazio"));
		}
	}

	@Test
	public void naoDeveAlugarFilmeSemFilme() throws FilmeSemEstoqueException, LocadoraException {

		Usuario usuario = umUsuario().agora();

		exception.expect(LocadoraException.class);
		exception.expectMessage("Filme vazio");

		// ação
		service.alugarFilme(usuario, null);
	}

	@Test
	public void deveDevolverNaSegundaAoAlugarNoSabado() throws Exception {

		// cenário
		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilme().agora());

		Mockito.doReturn(DataUtils.obterData(20, 11, 2021)).when(service).obterData();

		// ação
		Locacao retorno = service.alugarFilme(usuario, filmes);

		// verificação
		assertThat(retorno.getDataRetorno(), caiNumaSegunda());
	}

	/*
	 * public static void main(String[] args) { new
	 * BuilderMaster().gerarCodigoClasse(Locacao.class); }
	 */

	@Test
	public void naoDeveAlugarFilmeParaNegativadoSPC() throws Exception {
		// cenário
		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilme().agora());

		Mockito.when(spc.possuiNegativacao(Mockito.any(Usuario.class))).thenReturn(true);

		// ação
		try {
			service.alugarFilme(usuario, filmes);

			// verificação
			Assert.fail();
		} catch (LocadoraException e) {
			Assert.assertThat(e.getMessage(), is("Usuário negativado"));
		}

		Mockito.verify(spc).possuiNegativacao(usuario);
	}

	@Test
	public void deveEnviarEmailParaLocacoesAtrasadas() {
		// cenario
		Usuario usuario = umUsuario().agora();
		Usuario usuario2 = umUsuario().comNome("Usuário em dia").agora();
		Usuario usuario3 = umUsuario().comNome("Outro atrasado").agora();
		List<Locacao> locacoes = Arrays.asList(LocacaoBuilder.umLocacao().atrasado().comUsuario(usuario).agora(),
				LocacaoBuilder.umLocacao().comUsuario(usuario2).agora(),
				LocacaoBuilder.umLocacao().atrasado().comUsuario(usuario3).agora(),
				LocacaoBuilder.umLocacao().atrasado().comUsuario(usuario3).agora());
		Mockito.when(dao.obterLocacoesPendentes()).thenReturn(locacoes);

		// ação
		service.notificarAtrasos();

		// verificação
		verify(emailService, Mockito.times(3)).notificarAtraso(Mockito.any(Usuario.class));
		verify(emailService).notificarAtraso(usuario);
		// verify(emailService, Mockito.times(2)).notificarAtraso(usuario3);
		verify(emailService, Mockito.atLeast(1)).notificarAtraso(usuario3);
		verify(emailService, Mockito.never()).notificarAtraso(usuario2);
		verifyNoMoreInteractions(emailService);
	}

	@Test
	public void deveTratarErroNoSPC() throws Exception {
		// cenário
		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilme().agora());

		Mockito.when(spc.possuiNegativacao(usuario)).thenThrow(new RuntimeException("Falha catastrófica!"));

		// verificação
		exception.expect(LocadoraException.class);
		// exception.expectMessage("Falha catastrófica!");
		exception.expectMessage("Problemas com SPC, tente novamente");

		// ação
		service.alugarFilme(usuario, filmes);

	}

	@Test
	public void deveProrrogarUmaLocacao() {
		// cenário
		Locacao locacao = LocacaoBuilder.umLocacao().agora();

		// ação
		service.prorrogarLocacao(locacao, 3);

		// verificacao
		ArgumentCaptor<Locacao> argCapt = ArgumentCaptor.forClass(Locacao.class);
		Mockito.verify(dao).salvar(argCapt.capture());
		Locacao locacaoRetornada = argCapt.getValue();

		error.checkThat(locacaoRetornada.getValor(), is(12.0));
		error.checkThat(locacaoRetornada.getDataLocacao(), is(ehHoje()));
		error.checkThat(locacaoRetornada.getDataRetorno(), is(ehHojeComDiferencaDias(3)));
	}

	@Test
	public void deveCalcularValorLocacao() throws Exception {
		// cenário
		List<Filme> filmes = Arrays.asList(umFilme().agora());

		// ação
		Class<LocacaoService> clazz = LocacaoService.class;
		Method metodo = clazz.getDeclaredMethod("calcularValorLocacao", List.class);
		metodo.setAccessible(true);
		Double valor = (Double) metodo.invoke(service, filmes);

		// verificação
		Assert.assertThat(valor, is(4.0));
	}

}
