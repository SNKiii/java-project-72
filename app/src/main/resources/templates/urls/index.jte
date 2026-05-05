@import hexlet.code.UrlsPage
@param UrlsPage page

@template.layout(page = page, content = @`
    <h1>Сайты</h1>

    <table class="table table-bordered table-hover mt-3" data-test="urls">
        <thead>
        <tr>
            <th>ID</th>
            <th>Имя</th>
            <th>Дата создания</th>
            <th>Код ответа</th>
        </tr>
        </thead>
        <tbody>
        @for(url : page.getUrls())
            <tr>
                <td>${url.getId()}</td>
                <td>
                    <a href="/urls/${url.getId()}">${url.getName()}</a>
                </td>
                <td>${url.getCreatedAt()}</td>
                <td>
                    @if(page.getLastCheckCode().containsKey(url.getId()))
                        ${page.getLastCheckCode().get(url.getId())}
                    @else
                        -
                    @endif
                </td>
            </tr>
        @endfor
        </tbody>
    </table>
`)