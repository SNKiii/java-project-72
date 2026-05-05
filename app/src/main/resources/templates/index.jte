@import hexlet.code.BasePage
@param BasePage page = null

@template.layout(
page = page,
content = @`
    <div class="container-lg mt-5">
        <div class="row">
            <div class="col-12 col-md-10 col-lg-8 mx-auto border rounded-3 bg-light p-5">
                <h1 class="display-3 mb-0">Анализатор страниц</h1>
                <p class="lead">Бесплатно проверяйте сайты на SEO пригодность</p>

                @if(page != null && page.getError() != null)
                    <div class="alert alert-danger" role="alert">
                        ${page.getError()}
                    </div>
                @endif

                <form action="/urls" method="post" class="rss-form text-body">
                    <div class="row">
                        <div class="col">
                            <div class="form-floating">
                                <input
                                        id="url-input"
                                        type="text"
                                        name="url"
                                        class="form-control w-100"
                                        placeholder="Ссылка"
                                        autocomplete="off"
                                        required
                                >
                                <label for="url-input">Ссылка</label>
                            </div>
                        </div>
                        <div class="col-auto">
                            <button type="submit" class="h-100 btn btn-lg btn-primary px-sm-5 text-uppercase">Проверить</button>
                        </div>
                    </div>
                </form>

                <p class="mt-2 mb-0 text-muted">Пример: https://www.example.com</p>
            </div>
        </div>
    </div>
`
)

